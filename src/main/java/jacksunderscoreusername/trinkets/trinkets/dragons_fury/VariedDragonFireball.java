package jacksunderscoreusername.trinkets.trinkets.dragons_fury;

import jacksunderscoreusername.trinkets.Main;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;

import java.util.List;

public class VariedDragonFireball extends ExplosiveProjectileEntity {
    public VariedDragonFireball(EntityType<? extends VariedDragonFireball> entityType, World world) {
        super(entityType, world);
    }

    public static final EntityType<? extends VariedDragonFireball> VARIED_DRAGON_FIREBALL = register(
            "varied_dragon_fireball",
            EntityType.Builder.create(VariedDragonFireball::new, SpawnGroup.MISC)
                    .dropsNothing()
                    .dimensions(0.3125F, 0.3125F)
                    .eyeHeight(0.0F)
                    .maxTrackingRange(4)
                    .trackingTickInterval(10)
    );

    private static <T extends Entity> EntityType<T> register(RegistryKey<EntityType<?>> key, EntityType.Builder<T> type) {
        return Registry.register(Registries.ENTITY_TYPE, key, type.build(key));
    }

    private static <T extends Entity> EntityType<T> register(String id, EntityType.Builder<T> type) {
        return register(RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(Main.MOD_ID, id)), type);
    }

    public float radius = 0;
    public int duration = 0;
    public int amplifier = 0;

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
        if (hitResult.getType() != HitResult.Type.ENTITY || !this.isOwner(((EntityHitResult) hitResult).getEntity())) {
            if (!this.getWorld().isClient) {
                List<LivingEntity> list = this.getWorld().getNonSpectatingEntities(LivingEntity.class, this.getBoundingBox().expand(4.0, 2.0, 4.0));
                AreaEffectCloudEntity areaEffectCloudEntity = new AreaEffectCloudEntity(this.getWorld(), this.getX(), this.getY(), this.getZ());
                Entity entity = this.getOwner();
                if (entity instanceof LivingEntity) {
                    areaEffectCloudEntity.setOwner((LivingEntity) entity);
                }

                areaEffectCloudEntity.setParticleType(ParticleTypes.DRAGON_BREATH);
                float startRadius = Math.max(1, radius / 3);
                areaEffectCloudEntity.setRadius(startRadius);
                areaEffectCloudEntity.setDuration(duration);
                areaEffectCloudEntity.setRadiusGrowth((radius - startRadius) / (float) areaEffectCloudEntity.getDuration());
                areaEffectCloudEntity.addEffect(new StatusEffectInstance(StatusEffects.INSTANT_DAMAGE, 1, amplifier));
                if (!list.isEmpty()) {
                    for (LivingEntity livingEntity : list) {
                        double d = this.squaredDistanceTo(livingEntity);
                        if (d < 16.0) {
                            areaEffectCloudEntity.setPosition(livingEntity.getX(), livingEntity.getY(), livingEntity.getZ());
                            break;
                        }
                    }
                }

                this.getWorld().syncWorldEvent(WorldEvents.DRAGON_BREATH_CLOUD_SPAWNS, this.getBlockPos(), this.isSilent() ? -1 : 1);
                this.getWorld().spawnEntity(areaEffectCloudEntity);
                this.discard();
            }
        }
    }

    @Override
    protected ParticleEffect getParticleType() {
        return ParticleTypes.DRAGON_BREATH;
    }

    @Override
    protected boolean isBurning() {
        return false;
    }

    public static void initialize() {}
}
