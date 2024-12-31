package jacksunderscoreusername.trinkets;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import jacksunderscoreusername.trinkets.dialog.DialogPage;
import jacksunderscoreusername.trinkets.dialog.DialogScreenHandler;
import jacksunderscoreusername.trinkets.payloads.DialogClickedPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

public class DialogScreen extends HandledScreen<DialogScreenHandler> {
    private static final Gson gson = new Gson();
    private static final Identifier TEXTURE = Identifier.of(Main.MOD_ID, "textures/gui/container/dialog.png");
    public LivingEntity speakingEntity;
    private final PlayerEntity player;
    public DialogPage page;
    private LoadingWidget loadingWidget;

    public DialogScreen(DialogScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        player = inventory.player;
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        int y = this.y + 8;
        int x = this.x + 8 + 18 * 4;
        int maxWidth = this.x + this.backgroundWidth - x - 8;
        if (page != null) {
            if (loadingWidget != null) {
                loadingWidget.setPosition(this.width * 2, this.height * 2);
                loadingWidget = null;
            }
            int buttonIndex = 0;
            for (var item : page.items) {
                Text text = TextCodecs.CODEC.decode(JsonOps.INSTANCE, gson.fromJson(item.text, JsonElement.class)).getOrThrow().getFirst();
                if (item.isButton) {
                    int finalButtonIndex = buttonIndex;
                    ButtonWidget.Builder button = ButtonWidget.builder(text, (widget) -> {
                        ClientPlayNetworking.send(new DialogClickedPayload(finalButtonIndex));
                    });
                    if (item.tooltip != null) {
                        button.tooltip(Tooltip.of(TextCodecs.CODEC.decode(JsonOps.INSTANCE, gson.fromJson(item.text, JsonElement.class)).getOrThrow().getFirst()));
                    }
                    ButtonWidget widget = addDrawableChild(button.build());
                    widget.setWidth(textRenderer.getWidth(text) + 8);
                    widget.setHeight(textRenderer.getWrappedLinesHeight(text, Integer.MAX_VALUE) + 8);
                    widget.setPosition(x, y);
                    y += widget.getHeight();
                    buttonIndex++;
                } else {
                    ShadowlessMultilineTextWidget widget = new ShadowlessMultilineTextWidget(text, textRenderer);
                    widget.setMaxWidth(maxWidth);
                    widget.setPosition(x, y);
                    y += widget.getHeight();
                    addDrawable(widget);
                }
            }
        } else {
            loadingWidget = new LoadingWidget(textRenderer, Text.literal("Generating quest").formatted(Formatting.ITALIC));
            loadingWidget.setPosition(x, y);
            addDrawable(loadingWidget);
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;
        context.drawTexture(RenderLayer::getGuiTextured, TEXTURE, i, j, 0.0F, 0.0F, this.backgroundWidth, this.backgroundHeight, 256, 256);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
        if (speakingEntity != null) {
            Box entityBounds = speakingEntity.getBoundingBox(EntityPose.STANDING);
            Box playerBounds = player.getBoundingBox(EntityPose.STANDING);
            int size = Integer.min(Integer.min((int) (30 * playerBounds.getLengthX() / entityBounds.getLengthX()), (int) (30 * playerBounds.getLengthY() / entityBounds.getLengthY())), (int) (30 * playerBounds.getLengthZ() / entityBounds.getLengthZ()));
            InventoryScreen.drawEntity(context, this.x + 26, this.y + 8, this.x + 75, this.y + 78, size, 0.0625F, mouseX, mouseY, speakingEntity);
        }
    }

    @Override
    protected void init() {
        super.init();
        titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;
    }


    @Override
    public void close() {
        page = null;
        super.close();
    }
}
