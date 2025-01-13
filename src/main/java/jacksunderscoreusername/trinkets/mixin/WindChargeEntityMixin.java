package jacksunderscoreusername.trinkets.mixin;

import net.minecraft.entity.projectile.WindChargeEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.world.explosion.AdvancedExplosionBehavior;
import net.minecraft.world.explosion.ExplosionBehavior;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Optional;
import java.util.function.Function;

@Mixin(WindChargeEntity.class)
public class WindChargeEntityMixin implements jacksunderscoreusername.trinkets.minix_io.WindChargeEntity {
    @Unique
    public float trinkets_1_21_4_v2$knockbackMultiplier = 1.2F;

    @ModifyArg(
            method = "createExplosion",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;createExplosion(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/damage/DamageSource;Lnet/minecraft/world/explosion/ExplosionBehavior;DDDFZLnet/minecraft/world/World$ExplosionSourceType;Lnet/minecraft/particle/ParticleEffect;Lnet/minecraft/particle/ParticleEffect;Lnet/minecraft/registry/entry/RegistryEntry;)V"
            ),
            index = 2
    )
    private @NotNull ExplosionBehavior func(@Nullable ExplosionBehavior behavior) {
        return new AdvancedExplosionBehavior(
                true, false, Optional.of(this.trinkets_1_21_4_v2$knockbackMultiplier), Registries.BLOCK.getOptional(BlockTags.BLOCKS_WIND_CHARGE_EXPLOSIONS).map(Function.identity())
        );
    }

    @Override
    public void trinkets_1_21_4_v2$setKnockbackMultiplier(float multiplier) {
        this.trinkets_1_21_4_v2$knockbackMultiplier = multiplier;
    }
}
