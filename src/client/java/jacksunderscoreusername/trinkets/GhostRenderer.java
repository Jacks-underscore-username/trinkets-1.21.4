package jacksunderscoreusername.trinkets;

import jacksunderscoreusername.trinkets.trinkets.Trinkets;
import jacksunderscoreusername.trinkets.trinkets.soul_lamp.CursedEffect;
import jacksunderscoreusername.trinkets.trinkets.soul_lamp.Ghost;
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

public class GhostRenderer extends MobEntityRenderer<Ghost, VexEntityRenderState, VexEntityModel> {
    private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/illager/vex.png");
    private static final Identifier CHARGING_TEXTURE = Identifier.ofVanilla("textures/entity/illager/vex_charging.png");

    public GhostRenderer(EntityRendererFactory.Context context) {
        super(context, new VexEntityModel(context.getPart(EntityModelLayers.VEX)), 0.3F);
        this.addFeature(new HeldItemFeatureRenderer<>(this));
    }

    protected int getBlockLight(Ghost entity, BlockPos blockPos) {
        return 15;
    }

    public Identifier getTexture(VexEntityRenderState vexEntityRenderState) {
        return vexEntityRenderState.charging ? CHARGING_TEXTURE : TEXTURE;
    }

    public VexEntityRenderState createRenderState() {
        return new VexEntityRenderState();
    }

    public void updateRenderState(Ghost entity, VexEntityRenderState vexEntityRenderState, float f) {
        super.updateRenderState(entity, vexEntityRenderState, f);
        ArmedEntityRenderState.updateRenderState(entity, vexEntityRenderState, this.itemModelResolver);
        vexEntityRenderState.charging = entity.isCharging();
    }

    @Override
    public void render(VexEntityRenderState livingEntityRenderState, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null && (player.getStatusEffect(CursedEffect.CURSED) != null || player.getMainHandStack().isOf(Trinkets.SOUL_LAMP) || player.getOffHandStack().isOf(Trinkets.SOUL_LAMP))) {
            super.render(livingEntityRenderState, matrixStack, vertexConsumerProvider, i);
        }
    }
}
