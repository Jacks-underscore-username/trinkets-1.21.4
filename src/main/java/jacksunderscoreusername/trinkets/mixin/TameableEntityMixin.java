package jacksunderscoreusername.trinkets.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import jacksunderscoreusername.trinkets.trinkets.soul_lamp.GhostEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TameableEntity.class)
public class TameableEntityMixin {
    @WrapWithCondition(
            method = "onDeath",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerPlayerEntity;sendMessage(Lnet/minecraft/text/Text;)V"
            )
    )
    private boolean onDeath(ServerPlayerEntity instance, Text message) {
        return !((TameableEntity) (Object) this instanceof GhostEntity);
    }
}
