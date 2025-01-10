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
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class StateSaverAndLoader extends PersistentState {

    public static class StoredData implements Serializable {
        @Serial
        private static final long serialVersionUID = 4284636239471626404L;

        public HashMap<String, Integer> createdTrinkets = new HashMap<>();

        public static class currentTrinketPlayerMapEntry implements Serializable {
            public currentTrinketPlayerMapEntry(UUID player, int startTime) {
                this.player = player;
                this.startTime = startTime;
            }

            public UUID player;
            public int startTime;
        }

        public static class playerTrinketUseHistoryEntry implements Serializable {
            public playerTrinketUseHistoryEntry(int time, UUID itemUuid) {
                this.time = time;
                this.itemUuid = itemUuid;
            }

            public int time;
            public UUID itemUuid;
        }

        public HashMap<UUID, StateSaverAndLoader.StoredData.currentTrinketPlayerMapEntry> currentTrinketPlayerMap = new HashMap<>();
        public HashMap<UUID, UUID> claimedTrinketPlayerMap = new HashMap<>();
        public HashMap<UUID, ArrayList<StateSaverAndLoader.StoredData.playerTrinketUseHistoryEntry>> playerTrinketUseHistory = new HashMap<>();

        public record currentPlayerQuestsEntry(UUID playerUuid, UUID villagerUuid, String taskType,
                                               String encodedTask, int totalQuestProgress,
                                               UUID questUuid) implements Serializable {
        }

        public HashMap<UUID, ArrayList<currentPlayerQuestsEntry>> currentPlayerQuests = new HashMap<>();
    }

    public StoredData data = new StoredData();

    public static StateSaverAndLoader createFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        StateSaverAndLoader state = new StateSaverAndLoader();

        // Keep support for old saved
        if (tag.contains("data"))
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

        StoredData data = state.data;

        if (tag.contains("createdTrinkets")) {
            NbtCompound compound = tag.getCompound("createdTrinkets");
            for (var key : compound.getKeys())
                data.createdTrinkets.put(key, compound.getInt(key));
        }

        if (tag.contains("currentTrinketPlayerMap")) {
            NbtCompound compound = tag.getCompound("currentTrinketPlayerMap");
            for (var key : compound.getKeys()) {
                NbtCompound subCompound = compound.getCompound(key);
                data.currentTrinketPlayerMap.put(UUID.fromString(key), new StoredData.currentTrinketPlayerMapEntry(subCompound.getUuid("player"), subCompound.getInt("startTime")));
            }
        }

        if (tag.contains("claimedTrinketPlayerMap")) {
            NbtCompound compound = tag.getCompound("claimedTrinketPlayerMap");
            for (var key : compound.getKeys())
                data.claimedTrinketPlayerMap.put(UUID.fromString(key), compound.getUuid(key));
        }

        if (tag.contains("playerTrinketUseHistory")) {
            NbtCompound compound = tag.getCompound("playerTrinketUseHistory");
            for (var key : compound.getKeys()) {
                NbtCompound subCompound = compound.getCompound(key);
                ArrayList<StoredData.playerTrinketUseHistoryEntry> entry = new ArrayList<>();
                data.playerTrinketUseHistory.put(UUID.fromString(key), entry);
                for (var subKey : subCompound.getKeys()) {
                    NbtCompound subSubCompound = subCompound.getCompound(subKey);
                    entry.add(new StoredData.playerTrinketUseHistoryEntry(subSubCompound.getInt("time"), subSubCompound.getUuid("itemUuid")));
                }
            }
        }

        if (tag.contains("currentPlayerQuests")) {
            NbtCompound compound = tag.getCompound("currentPlayerQuests");
            for (var key : compound.getKeys()) {
                NbtCompound subCompound = compound.getCompound(key);
                ArrayList<StoredData.currentPlayerQuestsEntry> entry = new ArrayList<>();
                data.currentPlayerQuests.put(UUID.fromString(key), entry);
                for (var subKey : subCompound.getKeys()) {
                    NbtCompound subSubCompound = subCompound.getCompound(subKey);
                    entry.add(new StoredData.currentPlayerQuestsEntry(
                            subSubCompound.getUuid("playerUuid"),
                            subSubCompound.getUuid("villagerUuid"),
                            subSubCompound.getString("taskType"),
                            subSubCompound.getString("encodedTask"),
                            subSubCompound.getInt("totalQuestProgress"),
                            subSubCompound.getUuid("questUuid")
                    ));
                }
            }
        }

        return state;

    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        if (data.createdTrinkets != null) {
            NbtCompound compound = new NbtCompound();
            for (var entry : data.createdTrinkets.entrySet())
                compound.putInt(entry.getKey(), entry.getValue());
            nbt.put("createdTrinkets", compound);
        }

        if (data.currentTrinketPlayerMap != null) {
            NbtCompound compound = new NbtCompound();
            for (var entry : data.currentTrinketPlayerMap.entrySet()) {
                NbtCompound subCompound = new NbtCompound();
                subCompound.putUuid("player", entry.getValue().player);
                subCompound.putInt("startTime", entry.getValue().startTime);
                compound.put(entry.getKey().toString(), subCompound);
            }
            nbt.put("currentTrinketPlayerMap", compound);
        }

        if (data.claimedTrinketPlayerMap != null) {
            NbtCompound compound = new NbtCompound();
            for (var entry : data.claimedTrinketPlayerMap.entrySet())
                compound.putUuid(entry.getKey().toString(), entry.getValue());
            nbt.put("claimedTrinketPlayerMap", compound);
        }

        if (data.playerTrinketUseHistory != null) {
            NbtCompound compound = new NbtCompound();
            for (var entry : data.playerTrinketUseHistory.entrySet()) {
                NbtCompound subCompound = new NbtCompound();
                for (int i = 0; i < entry.getValue().size(); i++) {
                    NbtCompound subSubCompound = new NbtCompound();
                    subSubCompound.putUuid("itemUuid", entry.getValue().get(i).itemUuid);
                    subSubCompound.putInt("time", entry.getValue().get(i).time);
                    subCompound.put(String.valueOf(i), subSubCompound);
                }
                compound.put(entry.getKey().toString(), subCompound);
            }
            nbt.put("playerTrinketUseHistory", compound);
        }

        if (data.currentPlayerQuests != null) {
            NbtCompound compound = new NbtCompound();
            for (var entry : data.currentPlayerQuests.entrySet()) {
                NbtCompound subCompound = new NbtCompound();
                for (int i = 0; i < entry.getValue().size(); i++) {
                    NbtCompound subSubCompound = new NbtCompound();
                    StoredData.currentPlayerQuestsEntry subEntry = entry.getValue().get(i);
                    subSubCompound.putUuid("playerUuid", subEntry.playerUuid);
                    subSubCompound.putUuid("villagerUuid", subEntry.villagerUuid);
                    subSubCompound.putString("taskType", subEntry.taskType);
                    subSubCompound.putString("encodedTask", subEntry.encodedTask);
                    subSubCompound.putInt("totalQuestProgress", subEntry.totalQuestProgress);
                    subSubCompound.putUuid("questUuid", subEntry.questUuid);
                    subCompound.put(String.valueOf(i), subSubCompound);
                }
                compound.put(entry.getKey().toString(), subCompound);
            }
            nbt.put("currentPlayerQuests", compound);
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
