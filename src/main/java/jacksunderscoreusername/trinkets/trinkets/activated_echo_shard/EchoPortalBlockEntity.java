package jacksunderscoreusername.trinkets.trinkets.activated_echo_shard;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

// The blockEntity used for the echoPortal block to store the linked portal data.
public class EchoPortalBlockEntity extends BlockEntity {
    // Sets the default target for a portal since it has to have one set, however the color of the non setup portal will be black showing that it was never setup.
    public Identifier dimension = World.OVERWORLD.getValue();
    public BlockPos teleportPos = BlockPos.ORIGIN;
    // Color is stored as an RGB value, so 0 - 256**3-2
    public int colorInt = 0;
    public boolean checkForNetherPortal = false;

    public EchoPortalBlockEntity(BlockPos pos, BlockState state) {
        super(Setup.ECHO_PORTAL_BLOCK_ENTITY, pos, state);
    }

    // Used to store the data between world reloads.
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        dimension = Identifier.tryParse(nbt.getString("dimension"));
        teleportPos = BlockPos.fromLong(nbt.getLong("pos"));
        colorInt = nbt.getInt("color");
        checkForNetherPortal = nbt.getBoolean("checkForNetherPortal");
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        nbt.putString("dimension", dimension.toString());
        nbt.putLong("pos", teleportPos.asLong());
        nbt.putInt("color", colorInt);
        nbt.putBoolean("checkForNetherPortal", checkForNetherPortal);
    }

    @Override
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        return this.createNbtWithIdentifyingData(registries);
    }

    // Used for settings the portal's color elsewhere.
    @Override
    public Object getRenderData() {
        return this.colorInt;
    }
}
