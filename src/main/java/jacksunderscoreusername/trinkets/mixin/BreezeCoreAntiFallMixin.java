package jacksunderscoreusername.trinkets.mixin;

import jacksunderscoreusername.trinkets.Main;
import jacksunderscoreusername.trinkets.minix_io.BreezeCoreAntiFall;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(PlayerEntity.class)
public class BreezeCoreAntiFallMixin implements BreezeCoreAntiFall {
    @Unique
    private boolean trinkets_1_21_4_v2$BreezeCoreAntiFall = false;

    @Unique
    private long trinkets_1_21_4_v2$BreezeCoreAntiFallStart = 0;

    @Unique
    public void Trinkets_1_21_4_v2$setBreezeCoreAntiFall(MinecraftServer server) {
        trinkets_1_21_4_v2$BreezeCoreAntiFall = true;
        trinkets_1_21_4_v2$BreezeCoreAntiFallStart = server.getTicks();
    }

    @Inject(method = "handleFallDamage", at = @At("HEAD"), cancellable = true)
    public void handleFallDamage(CallbackInfoReturnable<Boolean> cir) {
        if (trinkets_1_21_4_v2$BreezeCoreAntiFall) cir.setReturnValue(false);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (!player.getWorld().isClient &&
                player.isOnGround() &&
                trinkets_1_21_4_v2$BreezeCoreAntiFall &&
                Objects.requireNonNull(player.getServer()).getTicks() - trinkets_1_21_4_v2$BreezeCoreAntiFallStart > 20)
            trinkets_1_21_4_v2$BreezeCoreAntiFall = false;
    }
}
