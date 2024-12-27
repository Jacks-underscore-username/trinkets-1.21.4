package jacksunderscoreusername.trinkets;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Hand;
import org.lwjgl.openal.LOKIIMAADPCM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    }
}