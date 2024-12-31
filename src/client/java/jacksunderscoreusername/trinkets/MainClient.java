package jacksunderscoreusername.trinkets;

import jacksunderscoreusername.trinkets.dialog.DialogPage;
import jacksunderscoreusername.trinkets.payloads.*;
import jacksunderscoreusername.trinkets.quest.QuestManager;
import jacksunderscoreusername.trinkets.trinkets.accursed_banner.Ghost;
import jacksunderscoreusername.trinkets.trinkets.dragons_fury.VariedDragonFireball;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;

import static jacksunderscoreusername.trinkets.trinkets.activated_echo_shard.Setup.ECHO_PORTAL;

public class MainClient implements ClientModInitializer {

    public static Config config = null;

    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(ECHO_PORTAL, RenderLayer.getTranslucent());
        ColorProviderRegistry.BLOCK.register((state, view, pos, tintIndex) -> {
            assert view != null;
            Object color = view.getBlockEntityRenderData(pos);
            if (color instanceof Integer) {
                return (int) color;
            } else return 0;
        }, ECHO_PORTAL);

        ClientPlayNetworking.registerGlobalReceiver(SwingHandPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                context.player().swingHand(payload.isMainHand() ? Hand.MAIN_HAND : Hand.OFF_HAND);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ConfigPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                Main.LOGGER.info("Received config packet");
                config = Config.fromJsonString(payload.configJson());
            });
        });

        EntityRendererRegistry.register(VariedDragonFireball.VARIED_DRAGON_FIREBALL, VariedDragonFireballRenderer::new);

        EntityRendererRegistry.register(Ghost.GHOST, GhostRenderer::new);

        HandledScreens.register(QuestManager.DIALOG_SCREEN_HANDLER, DialogScreen::new);

        ClientPlayNetworking.registerGlobalReceiver(SetDialogEntityPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                if (MinecraftClient.getInstance().currentScreen instanceof DialogScreen screen) {
                    Entity entity = MinecraftClient.getInstance().player.getWorld().getEntityById(payload.id());
                    if (entity instanceof LivingEntity livingEntity) {
                        screen.speakingEntity = livingEntity;
                    }
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(SendDialogPagePayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                if (MinecraftClient.getInstance().currentScreen instanceof DialogScreen screen) {
                    screen.items = DialogPage.decodeItems(payload.pageJson());
                    screen.init();
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(CloseDialogPagePayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                if (MinecraftClient.getInstance().currentScreen instanceof DialogScreen) {
                    MinecraftClient.getInstance().setScreen(null);
                }
            });
        });
    }
}