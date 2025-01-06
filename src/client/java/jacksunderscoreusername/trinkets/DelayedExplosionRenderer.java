package jacksunderscoreusername.trinkets;

import jacksunderscoreusername.trinkets.trinkets.fire_wand.DelayedExplosion;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.TntEntityRenderState;

public class DelayedExplosionRenderer extends EntityRenderer<DelayedExplosion, TntEntityRenderState> {

    public DelayedExplosionRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    public TntEntityRenderState createRenderState() {
        return new TntEntityRenderState();
    }
}
