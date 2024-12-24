package jacksunderscoreusername.trinkets.trinkets.activated_echo_shard;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class EchoPortalBlockEntity extends BlockEntity {
    public Identifier dimension = World.OVERWORLD.getValue();
    public BlockPos teleportPos = BlockPos.ORIGIN;
    public int colorInt = 0;
    public static int defaultColor = 0;

    public EchoPortalBlockEntity(BlockEntityType<? extends EchoPortalBlockEntity> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public EchoPortalBlockEntity(BlockPos pos, BlockState state) {
        super(SetupBlocks.ECHO_PORTAL_BLOCK_ENTITY, pos, state);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        dimension = Identifier.tryParse(nbt.getString("dimension"));
        teleportPos = BlockPos.fromLong(nbt.getLong("pos"));
        colorInt = nbt.getInt("color");
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        nbt.putString("dimension", dimension.toString());
        nbt.putLong("pos", teleportPos.asLong());
        nbt.putInt("color", colorInt);
    }

    @Override
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        return this.createNbtWithIdentifyingData(registries);
    }
}
