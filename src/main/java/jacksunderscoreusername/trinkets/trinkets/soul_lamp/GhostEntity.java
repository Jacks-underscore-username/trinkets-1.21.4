package jacksunderscoreusername.trinkets.trinkets.soul_lamp;

import jacksunderscoreusername.trinkets.Main;
import jacksunderscoreusername.trinkets.StateSaverAndLoader;
import jacksunderscoreusername.trinkets.events.LivingEntityDeathEvent;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class GhostEntity extends PathAwareEntity {
    public static final int field_28645 = MathHelper.ceil((float) (Math.PI * 5.0 / 4.0));
    protected static final TrackedData<Byte> GHOST_FLAGS = DataTracker.registerData(GhostEntity.class, TrackedDataHandlerRegistry.BYTE);
    private static final int CHARGING_FLAG = 1;
    public static final int ACTIVE_SEARCH_RADIUS = 50;
    public static final int PASSIVE_SEARCH_RADIUS = 25;
    @Nullable
    private BlockPos bounds;
    public StateSaverAndLoader.StoredData.soulLampEntry group;

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
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            if (this.group == null)
                for (var group : Main.state.data.soulLampGroups.values())
                    if (group.members.contains(this.getUuid()))
                        this.group = group;
            if (this.group != null && this.group.lifeTimeLeft == 0) {
                this.damage(serverWorld, this.getDamageSources().starve(), this.getMaxHealth());
            }
            if (this.group != null && !this.group.targets.isEmpty() && (this.getTarget() == null || !this.getTarget().isAlive())) {
                ArrayList<UUID> targets = new ArrayList<>(group.targets.stream().toList());
                Collections.shuffle(targets);
                for (var targetUuid : targets)
                    if (serverWorld.getEntity(targetUuid) != null && serverWorld.getEntity(targetUuid) instanceof LivingEntity target)
                        this.setTarget(target);
                    else
                        this.group.targets.remove(targetUuid);
            }
            if (this.group != null)
                for (var player : serverWorld.getPlayers()) {
                    if (!ghostsToRender.containsKey(player.getUuid()))
                        ghostsToRender.put(player.getUuid(), new HashSet<>());
                    if (this.group.playerUuid.equals(player.getUuid()) || this.group.targets.contains(player.getUuid()))
                        ghostsToRender.get(player.getUuid()).add(this.getId());
                }
        } else {
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
        }
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new LookAtEntityGoal(this, MobEntity.class, 8.0F));
        this.goalSelector.add(2, new LookAtEntityGoal(this, PlayerEntity.class, 3.0F, 1.0F));
        this.goalSelector.add(5, new LookAtTargetGoal());
        this.goalSelector.add(8, new GhostEntity.FollowOwnerGoal());
        this.goalSelector.add(30, new ChargeTargetGoal());
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
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (this.bounds != null) {
            nbt.putInt("BoundX", this.bounds.getX());
            nbt.putInt("BoundY", this.bounds.getY());
            nbt.putInt("BoundZ", this.bounds.getZ());
        }
    }

    public PlayerEntity getOwner() {
        if (this.getServer() != null &&
                this.group != null &&
                this.group.playerUuid != null &&
                this.getServer().getPlayerManager().getPlayer(this.group.playerUuid) != null) {
            return this.getServer().getPlayerManager().getPlayer(this.group.playerUuid);
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
    protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    @Override
    protected void tickCramming() {
    }

    @Override
    protected void updatePostDeath() {
        if (this.group != null)
            this.group.members.remove(this.getUuid());
        super.updatePostDeath();
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
                    if (GhostEntity.this.getTarget() == null || !GhostEntity.this.getTarget().isAlive()) {
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
            return (GhostEntity.this.group == null || GhostEntity.this.group.targets.isEmpty()) && getOwner() != null && GhostEntity.this.squaredDistanceTo(getOwner()) > 50;
        }

        @Override
        public boolean shouldRunEveryTick() {
            return true;
        }

        @Override
        public boolean shouldContinue() {
            return canStart();
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
                    if (GhostEntity.this.getTarget() == null || !GhostEntity.this.getTarget().isAlive()) {
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

    private static final HashMap<UUID, HashSet<Integer>> ghostsToRender = new HashMap<>();

    public static void initialize() {
        FabricDefaultAttributeRegistry.register(GHOST, HostileEntity.createHostileAttributes().add(EntityAttributes.MAX_HEALTH, 20.0).add(EntityAttributes.ATTACK_DAMAGE, 4.0).add(EntityAttributes.FOLLOW_RANGE, 100));
        LivingEntityDeathEvent.EVENT.register((entity) -> {
            if (entity.getAttacker() != null && entity.getAttacker() instanceof GhostEntity ghost && ghost.group != null && entity.getWorld() instanceof ServerWorld world) {
                for (int i = 0; i < ghost.group.soulMultiplier; i++) {
                    GhostEntity newGhost = GhostEntity.GHOST.spawn(world, entity.getBlockPos(), SpawnReason.MOB_SUMMONED);
                    assert newGhost != null;
                    ghost.group.members.add(newGhost.uuid);
                    newGhost.group = ghost.group;
                }
            }
            return ActionResult.PASS;
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            StateSaverAndLoader.StoredData.soulLampEntry group = null;
            LivingEntity targetEntity = null;
            if (!(source.getAttacker() instanceof LivingEntity) || !source.getAttacker().isAlive()) return true;
            if (entity instanceof GhostEntity ghost && ghost.group != null) {
                group = ghost.group;
                targetEntity = (LivingEntity) source.getAttacker();
            } else if (source.getAttacker() instanceof GhostEntity ghost && ghost.group != null) {
                group = ghost.group;
                targetEntity = entity;
            } else if (entity instanceof ServerPlayerEntity player && Main.state.data.soulLampGroups.values().stream().anyMatch(entry -> entry.playerUuid.equals(player.getUuid()))) {
                group = Main.state.data.soulLampGroups.values().stream().filter(entry -> entry.playerUuid.equals(player.getUuid())).toList().getFirst();
                targetEntity = (LivingEntity) source.getAttacker();
            } else if (source.getAttacker() instanceof ServerPlayerEntity player && Main.state.data.soulLampGroups.values().stream().anyMatch(entry -> entry.playerUuid.equals(player.getUuid()))) {
                group = Main.state.data.soulLampGroups.values().stream().filter(entry -> entry.playerUuid.equals(player.getUuid())).toList().getFirst();
                targetEntity = entity;
            }
            if (group == null || targetEntity == null) return true;
            if (targetEntity instanceof GhostEntity ghost && ghost.group != null && ghost.group.equals(group))
                return true;
            if (targetEntity instanceof ServerPlayerEntity player && group.playerUuid.equals(player.getUuid()))
                return true;
            group.targets.add(targetEntity.getUuid());
            return true;
        });
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            for (var entry : ghostsToRender.entrySet()) {
                UUID playerUuid = entry.getKey();
                if (world.getEntity(playerUuid) instanceof ServerPlayerEntity player) {
                    ServerPlayNetworking.send(player, new RenderGhostsPayload(new NbtIntArray(entry.getValue().stream().toList())));
                }
            }
            ghostsToRender.clear();
        });
        ServerTickEvents.START_WORLD_TICK.register(world -> {
            for (var group : Main.state.data.soulLampGroups.values()) {
                ServerPlayerEntity player = (ServerPlayerEntity) world.getPlayerByUuid(group.playerUuid);
                if (player == null) return;
                if (group.mode == 1) {
                Box box = new Box(player.getPos().subtract(ACTIVE_SEARCH_RADIUS), player.getPos().add(ACTIVE_SEARCH_RADIUS));
                    HashSet<EntityType<?>> types = new HashSet<>();
                    for (var targetUuid : group.targets) {
                        LivingEntity target = (LivingEntity) world.getEntity(targetUuid);
                        if (target == null) continue;
                        types.add(target.getType());
                    }
                    for (var type : types)
                        for (var newTarget : world.getEntitiesByType(type, box, entity -> !group.playerUuid.equals(entity.getUuid()) && !group.members.contains(entity.getUuid()) && !group.targets.contains(entity.getUuid())))
                            group.targets.add(newTarget.getUuid());
                } else if (group.mode == 3) {
                    Box box = new Box(player.getPos().subtract(PASSIVE_SEARCH_RADIUS), player.getPos().add(PASSIVE_SEARCH_RADIUS));
                    for (var newTarget : world.getEntitiesByClass(LivingEntity.class, box, entity -> !group.playerUuid.equals(entity.getUuid()) && !group.members.contains(entity.getUuid()) && !group.targets.contains(entity.getUuid())))
                        group.targets.add(newTarget.getUuid());
                }

            }
        });
    }
}
