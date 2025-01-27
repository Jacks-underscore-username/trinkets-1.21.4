package jacksunderscoreusername.ancient_trinkets.mixin;

import jacksunderscoreusername.ancient_trinkets.events.LivingEntityDeathEvent;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityDeathEventMixin {
    @Inject(method = "onDeath", at = @At("HEAD"))
    public void func(CallbackInfo ci) {
        LivingEntityDeathEvent.EVENT.invoker().interact((LivingEntity) (Object) this);
    }
}