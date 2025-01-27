package jacksunderscoreusername.ancient_trinkets;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.io.*;
import java.util.*;

public class StateSaverAndLoader extends PersistentState {

    public static Object soulLampGroupsSync = new Object();

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

        public static class lastTrinketLocationEntry implements Serializable {
            public lastTrinketLocationEntry(long time, GlobalPos pos) {
                this.time = time;
                this.pos = pos;
            }

            public long time;
            public GlobalPos pos;
        }

        public HashMap<UUID, StateSaverAndLoader.StoredData.currentTrinketPlayerMapEntry> currentTrinketPlayerMap = new HashMap<>();
        public HashMap<UUID, UUID> claimedTrinketPlayerMap = new HashMap<>();
        public HashMap<UUID, ArrayList<StateSaverAndLoader.StoredData.playerTrinketUseHistoryEntry>> playerTrinketUseHistory = new HashMap<>();
        public HashMap<UUID, lastTrinketLocationEntry> lastTrinketLocations = new HashMap<>();
        public HashMap<UUID, Integer> trinketCompasses = new HashMap<>();

        public record currentPlayerQuestsEntry(UUID playerUuid, UUID villagerUuid, String taskType,
                                               String encodedTask, int totalQuestProgress,
                                               UUID questUuid) implements Serializable {
        }

        public HashMap<UUID, ArrayList<currentPlayerQuestsEntry>> currentPlayerQuests = new HashMap<>();

        public static class soulLampEntry {
            public soulLampEntry(
                    UUID playerUuid, long lifeTimeLeft, int soulMultiplier, HashSet<UUID> targets, HashSet<UUID> priorityTargets,
                    HashSet<UUID> members, int mode) {
                this.playerUuid = playerUuid;
                this.lifeTimeLeft = lifeTimeLeft;
                this.soulMultiplier = soulMultiplier;
                this.targets = targets;
                this.priorityTargets = priorityTargets;
                this.members = members;
                this.mode = mode;
            }

            public UUID playerUuid;
            public long lifeTimeLeft;
            public int soulMultiplier;
            public HashSet<UUID> targets;
            public HashSet<UUID> priorityTargets;
            public HashSet<UUID> members;
            public int mode;
        }

        public HashMap<UUID, soulLampEntry> soulLampGroups = new HashMap<>();
    }

    public StoredData data = new StoredData();

    public static StateSaverAndLoader createFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        StateSaverAndLoader state = new StateSaverAndLoader();

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

        if (tag.contains("lastTrinketLocations")) {
            NbtCompound compound = tag.getCompound("lastTrinketLocations");
            for (var key : compound.getKeys()) {
                NbtCompound subCompound = compound.getCompound(key);
                data.lastTrinketLocations.put(UUID.fromString(key), new StoredData.lastTrinketLocationEntry(subCompound.getLong("time"), GlobalPos.CODEC.parse(NbtOps.INSTANCE, subCompound.get("pos")).getOrThrow()));
            }
        }

        if (tag.contains("trinketCompasses")) {
            NbtCompound compound = tag.getCompound("trinketCompasses");
            for (var key : compound.getKeys())
                data.trinketCompasses.put(UUID.fromString(key), compound.getInt(key));
        }

        if (tag.contains("soulLampGroups")) {
            NbtCompound compound = tag.getCompound("soulLampGroups");
            for (var key : compound.getKeys()) {
                NbtCompound subCompound = compound.getCompound(key);
                HashSet<UUID> targets = new HashSet<>();
                for (var subKey : subCompound.getCompound("targets").getKeys())
                    targets.add(UUID.fromString(subKey));
                HashSet<UUID> priorityTargets = new HashSet<>();
                for (var subKey : subCompound.getCompound("priorityTargets").getKeys())
                    priorityTargets.add(UUID.fromString(subKey));
                HashSet<UUID> members = new HashSet<>();
                for (var subKey : subCompound.getCompound("members").getKeys())
                    members.add(UUID.fromString(subKey));
                data.soulLampGroups.put(UUID.fromString(key), new StoredData.soulLampEntry(
                        subCompound.getUuid("playerUuid"),
                        subCompound.getLong("lifeTimeLeft"),
                        subCompound.getInt("soulMultiplier"),
                        targets,
                        priorityTargets,
                        members,
                        subCompound.getInt("mode")
                ));
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

        if (data.lastTrinketLocations != null) {
            NbtCompound compound = new NbtCompound();
            for (var entry : data.lastTrinketLocations.entrySet()) {
                NbtCompound subCompound = new NbtCompound();
                subCompound.putLong("time", entry.getValue().time);
                subCompound.put("pos", GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, entry.getValue().pos).getOrThrow());
                compound.put(entry.getKey().toString(), subCompound);
            }
            nbt.put("lastTrinketLocations", compound);
        }

        if (data.trinketCompasses != null) {
            NbtCompound compound = new NbtCompound();
            for (var entry : data.trinketCompasses.entrySet())
                compound.putInt(entry.getKey().toString(), entry.getValue());
            nbt.put("trinketCompasses", compound);
        }

        if (data.soulLampGroups != null) {
            NbtCompound compound = new NbtCompound();
            for (var entry : data.soulLampGroups.entrySet()) {
                NbtCompound subCompound = new NbtCompound();
                subCompound.putUuid("playerUuid", entry.getValue().playerUuid);
                subCompound.putLong("lifeTimeLeft", entry.getValue().lifeTimeLeft);
                subCompound.putInt("soulMultiplier", entry.getValue().soulMultiplier);
                NbtCompound targetsCompound = new NbtCompound();
                for (var target : entry.getValue().targets)
                    targetsCompound.putByte(target.toString(), (byte) 1);
                subCompound.put("targets", targetsCompound);
                NbtCompound priorityTargetsCompound = new NbtCompound();
                for (var target : entry.getValue().priorityTargets)
                    priorityTargetsCompound.putByte(target.toString(), (byte) 1);
                subCompound.put("priorityTargets", targetsCompound);
                NbtCompound membersCompound = new NbtCompound();
                for (var target : entry.getValue().members)
                    membersCompound.putByte(target.toString(), (byte) 1);
                subCompound.put("members", membersCompound);
                subCompound.putInt("mode", entry.getValue().mode);
                compound.put(entry.getKey().toString(), subCompound);
            }
            nbt.put("soulLampGroups", compound);
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

        ServerLifecycleEvents.BEFORE_SAVE.register((server2, flush, force) -> state.markDirty());

        return state;
    }
}
