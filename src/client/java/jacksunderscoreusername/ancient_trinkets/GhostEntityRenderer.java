package jacksunderscoreusername.ancient_trinkets;

import jacksunderscoreusername.ancient_trinkets.trinkets.soul_lamp.GhostEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.state.ArmedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class GhostEntityRenderer extends MobEntityRenderer<GhostEntity, GhostEntityRenderState, GhostEntityModel> {
    private static final Identifier TEXTURE = Identifier.of(Main.MOD_ID, "textures/entity/ghost.png");
    private static final Identifier CHARGING_TEXTURE = Identifier.of(Main.MOD_ID, "textures/entity/ghost_charging.png");

    public GhostEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new GhostEntityModel(context.getPart(EntityModelLayers.VEX)), 0);
        this.addFeature(new HeldItemFeatureRenderer<>(this));
    }

    protected int getBlockLight(GhostEntity entity, BlockPos blockPos) {
        return 15;
    }

    public Identifier getTexture(GhostEntityRenderState ghostEntityRenderState) {
        return ghostEntityRenderState.charging ? CHARGING_TEXTURE : TEXTURE;
    }

    public GhostEntityRenderState createRenderState() {
        return new GhostEntityRenderState();
    }

    public void updateRenderState(GhostEntity ghost, GhostEntityRenderState ghostEntityRenderState, float f) {
        super.updateRenderState(ghost, ghostEntityRenderState, f);
        ArmedEntityRenderState.updateRenderState(ghost, ghostEntityRenderState, this.itemModelResolver);
        ghostEntityRenderState.charging = ghost.isCharging();
        ghostEntityRenderState.shouldRender = MainClient.ghostsToRender.contains(ghost.getId());
    }

    @Override
    public void render(GhostEntityRenderState livingEntityRenderState, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        if (livingEntityRenderState.shouldRender) {
            super.render(livingEntityRenderState, matrixStack, vertexConsumerProvider, i);
        }
    }
}
