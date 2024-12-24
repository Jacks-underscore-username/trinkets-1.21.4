package jacksunderscoreusername.trinkets;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.io.*;
import java.util.ArrayList;
import java.util.Objects;

public class StateSaverAndLoader extends PersistentState {

    public static class StoredData implements Serializable {
        @Serial
        private static final long serialVersionUID = 4284636239471626404L;

        public ArrayList<String> createdTrinkets = new ArrayList<>();
    }

    public StoredData data = new StoredData();

    public static StateSaverAndLoader createFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        StateSaverAndLoader state = new StateSaverAndLoader();

        try {
            byte[] mapString = tag.getByteArray("data");
            InputStream inStream = new ByteArrayInputStream(mapString);
            ObjectInputStream in = new ObjectInputStream(inStream);

            state.data = (StoredData) in.readObject();

            in.close();
            inStream.close();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        try {
            byte[] mapBytes;

            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(byteStream);
            out.writeObject(data);

            out.flush();
            byteStream.flush();

            mapBytes = byteStream.toByteArray();

            out.close();
            byteStream.close();

            nbt.putByteArray("data", mapBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return nbt;
    }

    private static final Type<StateSaverAndLoader> type = new Type<>(StateSaverAndLoader::new,
            StateSaverAndLoader::createFromNbt,
            null
    );

    public static StateSaverAndLoader getServerState(MinecraftServer server) {
        PersistentStateManager persistentStateManager = Objects.requireNonNull(server.getWorld(World.OVERWORLD)).getPersistentStateManager();

        StateSaverAndLoader state = persistentStateManager.getOrCreate(type, Main.MOD_ID);

        ServerLifecycleEvents.BEFORE_SAVE.register((server2, flush, force) -> {
            state.markDirty();
        });

        return state;
    }
}
