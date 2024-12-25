package jacksunderscoreusername.trinkets;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.render.RenderLayer;

import static jacksunderscoreusername.trinkets.trinkets.activated_echo_shard.SetupBlocks.ECHO_PORTAL;

public class MainClient implements ClientModInitializer {
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
    }
}