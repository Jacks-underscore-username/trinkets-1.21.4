package jacksunderscoreusername.trinkets.trinkets.fire_wand;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.fluid.FluidState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class DelayedExplosion extends Entity implements Ownable {
    private static final TrackedData<Integer> FUSE = DataTracker.registerData(DelayedExplosion.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<BlockState> BLOCK_STATE = DataTracker.registerData(DelayedExplosion.class, TrackedDataHandlerRegistry.BLOCK_STATE);
    private static final TrackedData<Float> EXPLOSION_POWER = DataTracker.registerData(DelayedExplosion.class, TrackedDataHandlerRegistry.FLOAT);
    private static final ExplosionBehavior TELEPORTED_EXPLOSION_BEHAVIOR = new ExplosionBehavior() {
        @Override
        public boolean canDestroyBlock(Explosion explosion, BlockView world, BlockPos pos, BlockState state, float power) {
            return !state.isOf(Blocks.NETHER_PORTAL) && super.canDestroyBlock(explosion, world, pos, state, power);
        }

        @Override
        public Optional<Float> getBlastResistance(Explosion explosion, BlockView world, BlockPos pos, BlockState blockState, FluidState fluidState) {
            return blockState.isOf(Blocks.NETHER_PORTAL) ? Optional.empty() : super.getBlastResistance(explosion, world, pos, blockState, fluidState);
        }
    };
    @Nullable
    private LivingEntity causingEntity;
    private boolean teleported;

    public DelayedExplosion(EntityType<? extends DelayedExplosion> entityType, World world) {
        super(entityType, world);
        this.intersectionChecked = true;
    }

    public DelayedExplosion(World world, double x, double y, double z, @Nullable LivingEntity igniter, int fuse, float explosionPower) {
        this(FireWand.DELAYED_EXPLOSION, world);
        this.setPosition(x, y, z);
        this.setFuse(fuse);
        this.setExplosionPower(explosionPower);
        this.prevX = x;
        this.prevY = y;
        this.prevZ = z;
        this.causingEntity = igniter;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(FUSE, 0);
        builder.add(BLOCK_STATE, Blocks.TNT.getDefaultState());
        builder.add(EXPLOSION_POWER, 0F);
    }

    @Override
    protected Entity.MoveEffect getMoveEffect() {
        return Entity.MoveEffect.NONE;
    }

    @Override
    protected double getGravity() {
        return 0;
    }

    @Override
    public void tick() {
        int i = this.getFuse() - 1;
        this.setFuse(i);
        if (i <= 0) {
            this.discard();
            if (!this.getWorld().isClient) {
                this.explode();
            }
        }
    }

    private void explode() {
        this.getWorld()
                .createExplosion(
                        this,
                        Explosion.createDamageSource(this.getWorld(), this),
                        this.teleported ? TELEPORTED_EXPLOSION_BEHAVIOR : null,
                        this.getX(),
                        this.getBodyY(0.0625),
                        this.getZ(),
                        this.getExplosionPower(),
                        true,
                        World.ExplosionSourceType.MOB
                );
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putShort("fuse", (short) this.getFuse());
        nbt.put("block_state", NbtHelper.fromBlockState(this.getBlockState()));
        nbt.putFloat("explosion_power", this.getExplosionPower());
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.setFuse(nbt.getShort("fuse"));
        if (nbt.contains("block_state", NbtElement.COMPOUND_TYPE)) {
            this.setBlockState(NbtHelper.toBlockState(this.getWorld().createCommandRegistryWrapper(RegistryKeys.BLOCK), nbt.getCompound("block_state")));
        }

        if (nbt.contains("explosion_power", NbtElement.NUMBER_TYPE)) {
            this.setExplosionPower(MathHelper.clamp(nbt.getFloat("explosion_power"), 0.0F, 128.0F));
        }
    }

    @Nullable
    public LivingEntity getOwner() {
        return this.causingEntity;
    }

    @Override
    public void copyFrom(Entity original) {
        super.copyFrom(original);
        if (original instanceof DelayedExplosion tntEntity) {
            this.causingEntity = tntEntity.causingEntity;
        }
    }

    public void setFuse(int fuse) {
        this.dataTracker.set(FUSE, fuse);
    }

    public int getFuse() {
        return this.dataTracker.get(FUSE);
    }

    public void setBlockState(BlockState state) {
        this.dataTracker.set(BLOCK_STATE, state);
    }

    public BlockState getBlockState() {
        return this.dataTracker.get(BLOCK_STATE);
    }

    public void setExplosionPower(float power) {
        this.dataTracker.set(EXPLOSION_POWER, power);
    }

    public float getExplosionPower() {
        return this.dataTracker.get(EXPLOSION_POWER);
    }

    private void setTeleported(boolean teleported) {
        this.teleported = teleported;
    }

    @Nullable
    @Override
    public Entity teleportTo(TeleportTarget teleportTarget) {
        Entity entity = super.teleportTo(teleportTarget);
        if (entity instanceof DelayedExplosion tntEntity) {
            tntEntity.setTeleported(true);
        }

        return entity;
    }

    @Override
    public final boolean damage(ServerWorld world, DamageSource source, float amount) {
        return false;
    }
}
