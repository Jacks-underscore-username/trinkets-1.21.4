package jacksunderscoreusername.trinkets.trinkets.soul_lamp;

import jacksunderscoreusername.trinkets.Main;
import jacksunderscoreusername.trinkets.events.LivingEntityDeathEvent;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class GhostEntity extends TameableEntity {
    public static final int field_28645 = MathHelper.ceil((float) (Math.PI * 5.0 / 4.0));
    protected static final TrackedData<Byte> GHOST_FLAGS = DataTracker.registerData(GhostEntity.class, TrackedDataHandlerRegistry.BYTE);
    private static final int CHARGING_FLAG = 1;
    PlayerEntity owner;
    @Nullable
    private BlockPos bounds;
    private boolean alive;
    private int lifeTicks;
    private int maxLifeTicks;
    private int soulMultiplier;

    public GhostEntity(EntityType<? extends GhostEntity> entityType, World world) {
        super(entityType, world);
        this.moveControl = new GhostEntity.VexMoveControl(this);
        this.experiencePoints = 3;
    }

    public static final EntityType<GhostEntity> GHOST = register(RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(Main.MOD_ID, "ghost")),
            EntityType.Builder.create(GhostEntity::new, SpawnGroup.CREATURE)
                    .makeFireImmune()
                    .dimensions(0.4F, 0.8F)
                    .eyeHeight(0.51875F)
                    .passengerAttachments(0.7375F)
                    .vehicleAttachment(0.04F)
    );

    private static <T extends Entity> EntityType<T> register(RegistryKey<EntityType<?>> key, EntityType.Builder<T> type) {
        return Registry.register(Registries.ENTITY_TYPE, key, type.build(key));
    }

    @Override
    public boolean isFlappingWings() {
        return this.age % field_28645 == 0;
    }

    @Override
    protected boolean shouldTickBlockCollision() {
        return !this.isRemoved();
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public void tick() {
        this.noClip = true;
        super.tick();
        this.noClip = false;
        this.setNoGravity(true);
        this.setAir(this.getMaxAir());
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            if (this.isCharging()) {
                this.getWorld()
                        .addParticle(
                                ParticleTypes.SOUL,
                                this.getParticleX(0.5),
                                this.getRandomBodyY(),
                                this.getParticleZ(0.5),
                                (random.nextFloat() - .5) / 10, (random.nextFloat() - .5) / 10, (random.nextFloat() - .5) / 10
                        );
            } else if (this.random.nextInt(10) == 0) {
                this.getWorld()
                        .addParticle(
                                ParticleTypes.SOUL,
                                this.getParticleX(0.5),
                                this.getRandomBodyY(),
                                this.getParticleZ(0.5),
                                0, 0, 0
                        );
            }
        } else if (++this.lifeTicks > 0) {
            int healthLimit = 1 + (int) Math.ceil(this.getMaxHealth() - this.getMaxHealth() / this.maxLifeTicks * this.lifeTicks);
            if (this.getHealth() > healthLimit)
                this.damage(serverWorld, this.getDamageSources().starve(), 1);
        }
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(3, new GhostEntity.FollowOwnerGoal());
        this.goalSelector.add(6, new ChargeTargetGoal());
        this.goalSelector.add(8, new LookAtTargetGoal());
        this.goalSelector.add(9, new LookAtEntityGoal(this, PlayerEntity.class, 3.0F, 1.0F));
        this.goalSelector.add(10, new LookAtEntityGoal(this, MobEntity.class, 8.0F));
        this.targetSelector.add(1, new RevengeGoal(this).setGroupRevenge());
        this.targetSelector.add(5, new TrackOwnerAttackerGoal(this));
        this.targetSelector.add(10, new AttackWithOwnerGoal(this));
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(GHOST_FLAGS, (byte) 0);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("BoundX")) {
            this.bounds = new BlockPos(nbt.getInt("BoundX"), nbt.getInt("BoundY"), nbt.getInt("BoundZ"));
        }

        if (nbt.contains("LifeTicks")) {
            this.setLifeTicks(nbt.getInt("LifeTicks"));
        }

        if (nbt.contains("MaxLifeTicks")) {
            this.setMaxLifeTicks(nbt.getInt("MaxLifeTicks"));
        }

        if (nbt.contains("SoulMultiplier")) {
            this.setSoulMultiplier(nbt.getInt("SoulMultiplier"));
        }

        if (nbt.contains("Owner")) {
            this.setOwnerUuid(nbt.getUuid("Owner"));
        }
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return false;
    }

    private void copyData(Entity original) {
        if (original instanceof GhostEntity ghostEntity) {
            this.setOwnerUuid(ghostEntity.getOwnerUuid());
            this.lifeTicks = ghostEntity.lifeTicks;
            this.maxLifeTicks = ghostEntity.maxLifeTicks;
            this.soulMultiplier = ghostEntity.soulMultiplier;
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (this.bounds != null) {
            nbt.putInt("BoundX", this.bounds.getX());
            nbt.putInt("BoundY", this.bounds.getY());
            nbt.putInt("BoundZ", this.bounds.getZ());
        }

        if (this.alive) {
            nbt.putInt("LifeTicks", this.lifeTicks);
            nbt.putInt("MaxLifeTicks", this.maxLifeTicks);
            nbt.putInt("SoulMultiplier", this.soulMultiplier);
        }

        if (this.getOwnerUuid() != null) {
            nbt.putUuid("Owner", this.getOwnerUuid());
        }
    }

    public PlayerEntity getOwner() {
        if (this.owner != null)
            return this.owner;
        if (this.getServer() != null &&
                this.getOwnerUuid() != null &&
                this.getServer().getPlayerManager().getPlayer(this.getOwnerUuid()) != null) {
            this.owner = this.getServer().getPlayerManager().getPlayer(this.getOwnerUuid());
            return this.owner;
        }
        return null;
    }

    @Nullable
    public BlockPos getBounds() {
        return this.bounds;
    }

    private boolean areFlagsSet(int mask) {
        int i = this.dataTracker.get(GHOST_FLAGS);
        return (i & mask) != 0;
    }

    private void setVexFlag(int mask, boolean value) {
        int i = this.dataTracker.get(GHOST_FLAGS);
        if (value) {
            i |= mask;
        } else {
            i &= ~mask;
        }

        this.dataTracker.set(GHOST_FLAGS, (byte) (i & 0xFF));
    }

    public boolean isCharging() {
        return this.areFlagsSet(CHARGING_FLAG);
    }

    public void setCharging(boolean charging) {
        this.setVexFlag(CHARGING_FLAG, charging);
    }

    @Override
    public void setOwner(PlayerEntity player) {
        this.setTamed(true, true);
        this.setOwnerUuid(player.getUuid());
        this.owner = player;
    }

    public void setLifeTicks(int lifeTicks) {
        this.alive = true;
        this.lifeTicks = lifeTicks;
    }

    public void setMaxLifeTicks(int maxLifeTicks) {
        this.maxLifeTicks = maxLifeTicks;
    }

    public void setSoulMultiplier(int soulMultiplier) {
        this.soulMultiplier = soulMultiplier;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PARTICLE_SOUL_ESCAPE.value();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PARTICLE_SOUL_ESCAPE.value();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PARTICLE_SOUL_ESCAPE.value();
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
    public @Nullable PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return null;
    }

    @Override
    protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    @Override
    protected void tickCramming() {
    }

    class ChargeTargetGoal extends Goal {
        public ChargeTargetGoal() {
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            LivingEntity livingEntity = GhostEntity.this.getTarget();
            return livingEntity != null &&
                    livingEntity.isAlive() &&
                    !GhostEntity.this.getMoveControl().isMoving() &&
                    GhostEntity.this.random.nextInt(toGoalTicks(7)) == 0 &&
                    GhostEntity.this.squaredDistanceTo(livingEntity) > 4.0;
        }

        @Override
        public boolean shouldContinue() {
            return GhostEntity.this.getMoveControl().isMoving()
                    && GhostEntity.this.isCharging()
                    && GhostEntity.this.getTarget() != null
                    && GhostEntity.this.getTarget().isAlive();
        }

        @Override
        public void start() {
            LivingEntity livingEntity = GhostEntity.this.getTarget();
            if (livingEntity != null) {
                Vec3d vec3d = livingEntity.getEyePos();
                GhostEntity.this.moveControl.moveTo(vec3d.x, vec3d.y, vec3d.z, 1.0);
            }

            GhostEntity.this.setCharging(true);
            GhostEntity.this.playSound(SoundEvents.PARTICLE_SOUL_ESCAPE.value(), 1.0F, 1.0F);
        }

        @Override
        public void stop() {
            GhostEntity.this.setCharging(false);
        }

        @Override
        public boolean shouldRunEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity livingEntity = GhostEntity.this.getTarget();
            if (livingEntity != null) {
                if (GhostEntity.this.getBoundingBox().intersects(livingEntity.getBoundingBox())) {
                    GhostEntity.this.tryAttack(castToServerWorld(GhostEntity.this.getWorld()), livingEntity);
                    GhostEntity.this.setCharging(false);
                } else {
                    double d = GhostEntity.this.squaredDistanceTo(livingEntity);
                    if (d < 9.0) {
                        Vec3d vec3d = livingEntity.getEyePos();
                        GhostEntity.this.moveControl.moveTo(vec3d.x, vec3d.y, vec3d.z, 1.0);
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
            return !GhostEntity.this.getMoveControl().isMoving() && GhostEntity.this.random.nextInt(toGoalTicks(7)) == 0;
        }

        @Override
        public boolean shouldContinue() {
            return false;
        }

        @Override
        public void tick() {
            BlockPos blockPos = GhostEntity.this.getBounds();
            if (blockPos == null) {
                blockPos = GhostEntity.this.getBlockPos();
            }

            for (int i = 0; i < 3; i++) {
                BlockPos blockPos2 = blockPos.add(GhostEntity.this.random.nextInt(15) - 7, GhostEntity.this.random.nextInt(11) - 5, GhostEntity.this.random.nextInt(15) - 7);
                if (GhostEntity.this.getWorld().isAir(blockPos2)) {
                    GhostEntity.this.moveControl.moveTo((double) blockPos2.getX() + 0.5, (double) blockPos2.getY() + 0.5, (double) blockPos2.getZ() + 0.5, 0.25);
                    if (GhostEntity.this.getTarget() == null) {
                        GhostEntity.this.getLookControl().lookAt((double) blockPos2.getX() + 0.5, (double) blockPos2.getY() + 0.5, (double) blockPos2.getZ() + 0.5, 180.0F, 20.0F);
                    }
                    break;
                }
            }
        }
    }

    class FollowOwnerGoal extends Goal {

        @Override
        public boolean canStart() {
            return getTarget() == null;
        }

        @Override
        public boolean shouldRunEveryTick() {
            return true;
        }

        @Override
        public boolean shouldContinue() {
            return getTarget() == null;
        }

        @Override
        public void tick() {
            if (getOwner() != null) {
                double d = GhostEntity.this.squaredDistanceTo(getOwner());
                if (d > 50) {
                    Vec3d vec3d = getOwner().getEyePos().lerp(GhostEntity.this.getPos(), (GhostEntity.this.getWorld().random.nextDouble() * 5 + 45) / d);
                    GhostEntity.this.moveControl.moveTo(vec3d.x, vec3d.y, vec3d.z, 1.0);
                }
            }
        }
    }

    class VexMoveControl extends MoveControl {
        public VexMoveControl(final GhostEntity owner) {
            super(owner);
        }

        @Override
        public void tick() {
            if (this.state == MoveControl.State.MOVE_TO) {
                Vec3d vec3d = new Vec3d(this.targetX - GhostEntity.this.getX(), this.targetY - GhostEntity.this.getY(), this.targetZ - GhostEntity.this.getZ());
                double d = vec3d.length();
                if (d < GhostEntity.this.getBoundingBox().getAverageSideLength()) {
                    this.state = State.WAIT;
                    GhostEntity.this.setVelocity(GhostEntity.this.getVelocity().multiply(0.5));
                } else {
                    GhostEntity.this.setVelocity(GhostEntity.this.getVelocity().add(vec3d.multiply(this.speed * 0.05 / d)));
                    if (GhostEntity.this.getTarget() == null) {
                        Vec3d vec3d2 = GhostEntity.this.getVelocity();
                        GhostEntity.this.setYaw(-((float) MathHelper.atan2(vec3d2.x, vec3d2.z)) * (180.0F / (float) Math.PI));
                    } else {
                        double e = GhostEntity.this.getTarget().getX() - GhostEntity.this.getX();
                        double f = GhostEntity.this.getTarget().getZ() - GhostEntity.this.getZ();
                        GhostEntity.this.setYaw(-((float) MathHelper.atan2(e, f)) * (180.0F / (float) Math.PI));
                    }
                    GhostEntity.this.bodyYaw = GhostEntity.this.getYaw();
                }
            }
        }
    }

    public static void initialize() {
        FabricDefaultAttributeRegistry.register(GHOST, HostileEntity.createHostileAttributes().add(EntityAttributes.MAX_HEALTH, 20.0).add(EntityAttributes.ATTACK_DAMAGE, 4.0).add(EntityAttributes.FOLLOW_RANGE, 100));
        LivingEntityDeathEvent.EVENT.register((entity) -> {
            if (entity.getAttacker() != null && entity.getAttacker() instanceof GhostEntity ghost && entity.getWorld() instanceof ServerWorld world) {
                for (int i = 0; i < ghost.soulMultiplier; i++) {
                    GhostEntity.GHOST.spawn(world, entity.getBlockPos(), SpawnReason.MOB_SUMMONED).copyData(ghost);
                }
            }
            return ActionResult.PASS;
        });
    }
}
