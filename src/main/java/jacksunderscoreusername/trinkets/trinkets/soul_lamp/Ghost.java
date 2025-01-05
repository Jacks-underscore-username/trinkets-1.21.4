package jacksunderscoreusername.trinkets.trinkets.soul_lamp;

import jacksunderscoreusername.trinkets.Main;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class Ghost extends VexEntity {
    String sourceEntity;

    public Ghost(EntityType<? extends Ghost> entityType, World world) {
        super(entityType, world);
    }

    public static final EntityType<Ghost> GHOST = register(RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(Main.MOD_ID, "ghost")),
            EntityType.Builder.create(Ghost::new, SpawnGroup.MONSTER)
                    .makeFireImmune()
                    .dimensions(0.4F, 0.8F)
                    .eyeHeight(0.51875F)
                    .passengerAttachments(0.7375F)
                    .vehicleAttachment(0.04F)
                    .maxTrackingRange(100)
    );

    private static <T extends Entity> EntityType<T> register(RegistryKey<EntityType<?>> key, EntityType.Builder<T> type) {
        return Registry.register(Registries.ENTITY_TYPE, key, type.build(key));
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData) {
        Random random = world.getRandom();
        this.initEquipment(random, difficulty);
        this.updateEnchantments(world, random, difficulty);
        return super.initialize(world, difficulty, spawnReason, entityData);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(4, new Ghost.ChargeTargetGoal());
        this.goalSelector.add(8, new Ghost.LookAtTargetGoal());
        this.targetSelector.add(1, new RevengeGoal(this).setGroupRevenge());
        this.targetSelector.add(3, new ActiveCursedTargetGoal<>(this, true));
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        if (source.getAttacker() instanceof LivingEntity livingEntity) {
            livingEntity.addStatusEffect(new StatusEffectInstance(CursedEffect.CURSED, 30 * 20));
        }
        return super.damage(world, source, amount);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient) {
            if (this.random.nextInt(10) == 0 || this.isCharging()) {
                this.getWorld()
                        .addParticle(
                                ParticleTypes.SOUL,
                                this.getParticleX(0.5),
                                this.getRandomBodyY(),
                                this.getParticleZ(0.5),
                                0, 0, 0
                        );
            }
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("LifeTicks")) {
            this.sourceEntity= (nbt.getString("SourceEntity"));
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (this.sourceEntity!=null) {
            nbt.putString("SourceEntity", this.sourceEntity);
        }
    }

    class ChargeTargetGoal extends Goal {
        public ChargeTargetGoal() {
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            LivingEntity livingEntity = getTarget();
            return livingEntity != null && livingEntity.isAlive() && !getMoveControl().isMoving() && random.nextInt(toGoalTicks(7)) == 0 && squaredDistanceTo(livingEntity) > 4.0;
        }

        @Override
        public boolean shouldContinue() {
            return getMoveControl().isMoving()
                    && isCharging()
                    && getTarget() != null
                    && getTarget().isAlive()
                    && getTarget().getStatusEffect(CursedEffect.CURSED) != null;
        }

        @Override
        public void start() {
            LivingEntity livingEntity = getTarget();
            if (livingEntity != null) {
                Vec3d vec3d = livingEntity.getEyePos();
                moveControl.moveTo(vec3d.x, vec3d.y, vec3d.z, 1.0);
            }

            setCharging(true);
            playSound(SoundEvents.PARTICLE_SOUL_ESCAPE.value(), 2.0F, 1.0F);
        }

        @Override
        public void stop() {
            setTarget(null);
            setCharging(false);
        }

        @Override
        public boolean shouldRunEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity livingEntity = getTarget();
            if (livingEntity != null) {
                if (getBoundingBox().intersects(livingEntity.getBoundingBox())) {
                    tryAttack(castToServerWorld(getWorld()), livingEntity);
                    setCharging(false);
                } else {
                    double d = squaredDistanceTo(livingEntity);
                    if (d < 9.0) {
                        Vec3d vec3d = livingEntity.getEyePos();
                        moveControl.moveTo(vec3d.x, vec3d.y, vec3d.z, 1.0);
                    }
                }
            }
        }
    }

    class LookAtTargetGoal extends Goal {
        public LookAtTargetGoal() {
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            return !getMoveControl().isMoving() && random.nextInt(toGoalTicks(7)) == 0;
        }

        @Override
        public boolean shouldContinue() {
            return false;
        }

        @Override
        public void tick() {
            BlockPos blockPos = getBounds();
            if (blockPos == null) {
                blockPos = getBlockPos();
            }

            for (int i = 0; i < 3; i++) {
                BlockPos blockPos2 = blockPos.add(random.nextInt(15) - 7, random.nextInt(11) - 5, random.nextInt(15) - 7);
                if (getWorld().isAir(blockPos2)) {
                    moveControl.moveTo((double) blockPos2.getX() + 0.5, (double) blockPos2.getY() + 0.5, (double) blockPos2.getZ() + 0.5, 0.25);
                    if (getTarget() == null) {
                        getLookControl().lookAt((double) blockPos2.getX() + 0.5, (double) blockPos2.getY() + 0.5, (double) blockPos2.getZ() + 0.5, 180.0F, 20.0F);
                    }
                    break;
                }
            }
        }
    }

    public static void initialize() {
        FabricDefaultAttributeRegistry.register(GHOST, HostileEntity.createHostileAttributes().add(EntityAttributes.MAX_HEALTH, 20.0).add(EntityAttributes.ATTACK_DAMAGE, 4.0).add(EntityAttributes.FOLLOW_RANGE, 100));
    }
}
