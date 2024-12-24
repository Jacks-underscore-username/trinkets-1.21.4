package jacksunderscoreusername.trinkets;

import jacksunderscoreusername.trinkets.trinkets.activated_echo_shard.EchoPortalBlockEntity;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.RenderLayer;

import static jacksunderscoreusername.trinkets.trinkets.activated_echo_shard.SetupBlocks.ECHO_PORTAL;

public class MainClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.

		BlockRenderLayerMap.INSTANCE.putBlock(ECHO_PORTAL, RenderLayer.getTranslucent());
		ColorProviderRegistry.BLOCK.register((state, view, pos, tintIndex) -> {
			BlockEntity blockEntity = view.getBlockEntity(pos);

			if (blockEntity instanceof EchoPortalBlockEntity) {
				return ((EchoPortalBlockEntity) blockEntity).colorInt;
			} else return 0;

		}, ECHO_PORTAL);

	}
}