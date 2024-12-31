package jacksunderscoreusername.trinkets;

import com.google.gson.Gson;
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
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;

public class DialogScreen extends HandledScreen<DialogScreenHandler> {
    private static final Gson gson = new Gson();
    private static final Identifier TEXTURE = Identifier.of(Main.MOD_ID, "textures/gui/container/dialog.png");
    public LivingEntity speakingEntity;
    private final PlayerEntity player;
    public List<DialogPage.DialogPageItem> items = new ArrayList<>();
    private LoadingWidget loadingWidget;

    public DialogScreen(DialogScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        player = inventory.player;
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;
        context.drawTexture(RenderLayer::getGuiTextured, TEXTURE, i, j, 0.0F, 0.0F, this.backgroundWidth, this.backgroundHeight, 256, 256);
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
        int minX = this.x + 8 + 18 * 4;
        int minY = this.y + 8;
        int maxX = this.x + this.backgroundWidth - 8;
        int maxY = this.y + this.backgroundHeight - 8 - 18 - 8 - 18 * 3;
        if (!items.isEmpty()) {
            if (loadingWidget != null) {
                loadingWidget.setPosition(this.width * 2, this.height * 2);
                loadingWidget = null;
            }
            int buttonIndex = 0;
            for (var item : items) {
                ClickableWidget widget = null;
                if (item.getType().equals(DialogPage.Type.TEXT)) {
                    ShadowlessMultilineTextWidget subWidget = new ShadowlessMultilineTextWidget(item.getText(), textRenderer);
                    subWidget.setMaxWidth(maxX - minX);
                    widget = subWidget;
                }
                if (item.getType().equals(DialogPage.Type.BUTTON)) {
                    int finalButtonIndex = buttonIndex;
                    ButtonWidget.Builder button = ButtonWidget.builder(item.getText(), (w) -> {
                        ClientPlayNetworking.send(new DialogClickedPayload(finalButtonIndex));
                    });
                    if (item.getTooltip() != null) {
                        button.tooltip(Tooltip.of(item.getTooltip()));
                    }
                    ButtonWidget subWidget = button.build();
                    subWidget.setWidth(textRenderer.getWidth(item.getText()) + 8);
                    subWidget.setHeight(textRenderer.getWrappedLinesHeight(item.getText(), Integer.MAX_VALUE) + 8);
                    widget = subWidget;
                    buttonIndex++;
                }
                if (widget == null) {
                    throw new RuntimeException("Invalid widget type");
                }
                if (item.getAlignment().equals(DialogPage.Alignment.BOTTOM)) {
                    maxY -= widget.getHeight();
                    widget.setPosition(minX, maxY);
                } else {
                    widget.setPosition(minX, minY);
                    minY += widget.getHeight();
                }
                if (item.isClickable()) {
                    addDrawableChild(widget);
                } else {
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
    public void close() {
        items = new ArrayList<>();
        loadingWidget = null;
        super.close();
    }
}
