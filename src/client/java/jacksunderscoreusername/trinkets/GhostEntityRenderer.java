package jacksunderscoreusername.trinkets;

import jacksunderscoreusername.trinkets.trinkets.Trinkets;
import jacksunderscoreusername.trinkets.trinkets.soul_lamp.GhostEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.VexEntityModel;
import net.minecraft.client.render.entity.state.ArmedEntityRenderState;
import net.minecraft.client.render.entity.state.VexEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class GhostEntityRenderer extends MobEntityRenderer<GhostEntity, VexEntityRenderState, VexEntityModel> {
    private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/illager/vex.png");
    private static final Identifier CHARGING_TEXTURE = Identifier.ofVanilla("textures/entity/illager/vex_charging.png");

    public GhostEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new VexEntityModel(context.getPart(EntityModelLayers.VEX)), 0);
        this.addFeature(new HeldItemFeatureRenderer<>(this));
    }

    protected int getBlockLight(GhostEntity entity, BlockPos blockPos) {
        return 15;
    }

    public Identifier getTexture(VexEntityRenderState vexEntityRenderState) {
        return vexEntityRenderState.charging ? CHARGING_TEXTURE : TEXTURE;
    }

    public VexEntityRenderState createRenderState() {
        return new VexEntityRenderState();
    }

    public void updateRenderState(GhostEntity entity, VexEntityRenderState vexEntityRenderState, float f) {
        super.updateRenderState(entity, vexEntityRenderState, f);
        ArmedEntityRenderState.updateRenderState(entity, vexEntityRenderState, this.itemModelResolver);
        vexEntityRenderState.charging = entity.isCharging();
    }

    @Override
    public void render(VexEntityRenderState livingEntityRenderState, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null && (player.getMainHandStack().isOf(Trinkets.SOUL_LAMP) || player.getOffHandStack().isOf(Trinkets.SOUL_LAMP))) {
            super.render(livingEntityRenderState, matrixStack, vertexConsumerProvider, i);
        }
    }
}
