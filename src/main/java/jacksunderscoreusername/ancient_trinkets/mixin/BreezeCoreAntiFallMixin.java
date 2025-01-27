package jacksunderscoreusername.ancient_trinkets.mixin;

import jacksunderscoreusername.ancient_trinkets.minix_io.BreezeCoreAntiFall;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sound.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(PlayerEntity.class)
public abstract class BreezeCoreAntiFallMixin implements BreezeCoreAntiFall {
    @Shadow
    public abstract void playSound(SoundEvent sound, float volume, float pitch);

    @Unique
    private boolean trinkets_1_21_4_v2$BreezeCoreAntiFall = false;

    @Unique
    private long trinkets_1_21_4_v2$BreezeCoreAntiFallStart = 0;

    @Unique
    private long trinkets_1_21_4_v2$BreezeCoreTimeOnGround = 0;

    @Unique
    public void Trinkets_1_21_4_v2$setBreezeCoreAntiFall(MinecraftServer server) {
        trinkets_1_21_4_v2$BreezeCoreAntiFall = true;
        trinkets_1_21_4_v2$BreezeCoreAntiFallStart = server.getTicks();
        trinkets_1_21_4_v2$BreezeCoreTimeOnGround = 0;
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
            if (player.getServer().getTicks() - trinkets_1_21_4_v2$BreezeCoreTimeOnGround > 10 && trinkets_1_21_4_v2$BreezeCoreTimeOnGround != 0)
                trinkets_1_21_4_v2$BreezeCoreAntiFall = false;
            else if (trinkets_1_21_4_v2$BreezeCoreTimeOnGround == 0)
                trinkets_1_21_4_v2$BreezeCoreTimeOnGround = player.getServer().getTicks();
    }
}
