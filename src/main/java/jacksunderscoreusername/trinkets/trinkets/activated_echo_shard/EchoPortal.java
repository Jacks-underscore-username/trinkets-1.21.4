package jacksunderscoreusername.trinkets.trinkets.activated_echo_shard;

import com.mojang.serialization.MapCodec;
import jacksunderscoreusername.trinkets.Main;
import jacksunderscoreusername.trinkets.Utils;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.*;
import net.minecraft.world.dimension.NetherPortal;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

// It has to extend BlockWithEntity to have block data, but also needs to implement portal to be used as a portal without doing everything itself.
public class EchoPortal extends BlockWithEntity implements Portal {
    public static final MapCodec<EchoPortal> CODEC = createCodec(EchoPortal::new);

    public EchoPortal(Settings settings) {
        super(settings);
        // It needs a default state, so it just chooses a direction and has that as default.
        this.setDefaultState(this.stateManager.getDefaultState().with(Properties.HORIZONTAL_AXIS, Direction.Axis.X));
    }

    // Make entities treat this like a portal.
    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        // Check if the entity can even use portals.
        if (!entity.canUsePortals(false)) {
            return;
        }

        // The client cannot run the actual portal conditions code, so it just always tries to teleport so that distortion still works.
        if (world.isClient) {
            entity.tryUsePortal(this, pos);
            return;
        }

        // A block entity from the current portal with data on the target.
        EchoPortalBlockEntity blockEntity = ((EchoPortalBlockEntity) world.getBlockEntity(pos));
        assert blockEntity != null;

        // The block of the current portal.
        BlockState blockState = world.getBlockState(pos);

        // The world the other portal is in (if it exists).
        World otherWorld = Main.server.getWorld(RegistryKey.of(RegistryKeys.WORLD, blockEntity.dimension));
        assert otherWorld != null;

        // A block from the other portal (if it exists).
        BlockState targetPortalBlock = otherWorld.getBlockState(blockEntity.teleportPos);

        // If there is no portal at the saved location destroy this portal and abort.
        if (!(targetPortalBlock.getBlock() instanceof EchoPortal)) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState());
            return;
        }

        // A block entity from the other portal.
        EchoPortalBlockEntity otherBlockEntity = ((EchoPortalBlockEntity) otherWorld.getBlockEntity(blockEntity.teleportPos));
        assert otherBlockEntity != null;

        // If the other portal's block entity is linked with a portal that is not this portal, destroy this portal.
        Direction.Axis axis = (blockState.getOrEmpty(Properties.HORIZONTAL_AXIS).isEmpty() ? blockState.getBlock().getDefaultState().get(Properties.HORIZONTAL_AXIS) : blockState.get(Properties.HORIZONTAL_AXIS)).equals(Direction.Axis.X) ? Direction.Axis.Z : Direction.Axis.X;
        if (!Utils.areBothPointsConnected(pos, world.getRegistryKey(), otherBlockEntity.teleportPos, RegistryKey.of(RegistryKeys.WORLD, otherBlockEntity.dimension), Setup.ECHO_PORTAL, axis)) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState());
            return;
        }

        // Otherwise the portals are still both there and the teleport can happen.
        entity.tryUsePortal(this, pos);
    }

    // Copied from the netherPortalBlock code, sets the amount of time the entity has to be in the portal before they teleport.
    @Override
    public int getPortalDelay(ServerWorld world, Entity entity) {
        return entity instanceof PlayerEntity playerEntity
                ? Math.max(
                0,
                world.getGameRules()
                        .getInt(playerEntity.getAbilities().invulnerable ? GameRules.PLAYERS_NETHER_PORTAL_CREATIVE_DELAY : GameRules.PLAYERS_NETHER_PORTAL_DEFAULT_DELAY)
        )
                : 0;
    }

    // A whole ton of stuff to make the portal portal.
    @Override
    public @Nullable TeleportTarget createTeleportTarget(ServerWorld world, Entity entity, BlockPos pos) {
        EchoPortalBlockEntity info = ((EchoPortalBlockEntity) world.getBlockEntity(pos));

        ServerWorld teleportWorld = world.getServer().getWorld(RegistryKey.of(RegistryKeys.WORLD, info.dimension));
        BlockState otherPortal = teleportWorld.getBlockState(info.teleportPos);
        BlockState thisPortal = world.getBlockState(pos);

        TeleportTarget.PostDimensionTransition postDimensionTransition = TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET.then(entityX -> entityX.addPortalChunkTicketAt(info.teleportPos));

        Direction.Axis thisAxis = thisPortal.getOrEmpty(Properties.HORIZONTAL_AXIS).orElse(Direction.Axis.X);
        BlockLocating.Rectangle thisRectangle = BlockLocating.getLargestRectangle(
                pos, thisAxis, 21, Direction.Axis.Y, 21, posX -> world.getBlockState(posX) == thisPortal
        );

        Direction.Axis otherAxis = otherPortal.getOrEmpty(Properties.HORIZONTAL_AXIS).orElse(Direction.Axis.X);
        BlockLocating.Rectangle exitPortalRectangle = BlockLocating.getLargestRectangle(
                info.teleportPos, otherAxis, 21, Direction.Axis.Y, 21, posX -> teleportWorld.getBlockState(posX) == otherPortal
        );

        return getExitPortalTarget(teleportWorld, exitPortalRectangle, thisAxis, otherAxis, entity.positionInPortal(thisAxis, thisRectangle), entity, postDimensionTransition);
    }

    private static TeleportTarget getExitPortalTarget(
            ServerWorld world,
            BlockLocating.Rectangle exitPortalRectangle,
            Direction.Axis thisAxis,
            Direction.Axis otherAxis,
            Vec3d positionInPortal,
            Entity entity,
            TeleportTarget.PostDimensionTransition postDimensionTransition
    ) {

        BlockPos blockPos = exitPortalRectangle.lowerLeft;
        double d = exitPortalRectangle.width;
        double e = exitPortalRectangle.height;
        EntityDimensions entityDimensions = entity.getDimensions(entity.getPose());
        int i = thisAxis == otherAxis ? 0 : 90;
        double f = (double) entityDimensions.width() / 2.0 + (d - (double) entityDimensions.width()) * positionInPortal.getX();
        double g = (e - (double) entityDimensions.height()) * positionInPortal.getY();
        double h = 0.5 + positionInPortal.getZ();
        boolean bl = otherAxis == Direction.Axis.X;
        Vec3d vec3d = new Vec3d((double) blockPos.getX() + (bl ? f : h), (double) blockPos.getY() + g, (double) blockPos.getZ() + (bl ? h : f));
        return new TeleportTarget(world, vec3d, Vec3d.ZERO, (float) i, 0.0F, PositionFlag.combine(PositionFlag.DELTA, PositionFlag.ROT), postDimensionTransition);
    }

    @Override
    protected BlockState getStateForNeighborUpdate(
            BlockState state,
            WorldView world,
            ScheduledTickView tickView,
            BlockPos pos,
            Direction direction,
            BlockPos neighborPos,
            BlockState neighborState,
            Random random
    ) {
        Direction.Axis axis = direction.getAxis();
        Direction.Axis axis2 = state.get(Properties.HORIZONTAL_AXIS);
        boolean bl = axis2 != axis && axis.isHorizontal();

        if (!((EchoPortalBlockEntity) Objects.requireNonNull(world.getBlockEntity(pos))).checkForNetherPortal) {
            return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
        } else {
            return !bl && !neighborState.isOf(this) && !NetherPortal.getOnAxis(world, pos, axis2).wasAlreadyValid()
                    ? Blocks.AIR.getDefaultState()
                    : super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
        }
    }

    @Override
    protected BlockState rotate(BlockState state, BlockRotation rotation) {
        return switch (rotation) {
            case COUNTERCLOCKWISE_90, CLOCKWISE_90 -> switch (state.get(Properties.HORIZONTAL_AXIS)) {
                case Z -> state.with(Properties.HORIZONTAL_AXIS, Direction.Axis.X);
                case X -> state.with(Properties.HORIZONTAL_AXIS, Direction.Axis.Z);
                default -> state;
            };
            default -> state;
        };
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Properties.HORIZONTAL_AXIS);
    }

    // Stuff to make blockEntities work.
    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new EchoPortalBlockEntity(pos, state);
    }

    // Make it so it looks like you're in a portal when you're in the portal.
    @Override
    public Effect getPortalEffect() {
        return Effect.CONFUSION;
    }

    // Make the blocks hitbox work.
    protected static final VoxelShape X_SHAPE = Block.createCuboidShape(0.0, 0.0, 6.0, 16.0, 16.0, 10.0);
    protected static final VoxelShape Z_SHAPE = Block.createCuboidShape(6.0, 0.0, 0.0, 10.0, 16.0, 16.0);
    public static final EnumProperty<Direction.Axis> AXIS = Properties.HORIZONTAL_AXIS;
    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        switch (state.get(AXIS)) {
            case Z:
                return Z_SHAPE;
            case X:
            default:
                return X_SHAPE;
        }
    }
}