package jacksunderscoreusername.trinkets.quest;

import jacksunderscoreusername.trinkets.Main;
import jacksunderscoreusername.trinkets.StateSaverAndLoader;
import jacksunderscoreusername.trinkets.quest.tasks.GiveItem;
import jacksunderscoreusername.trinkets.quest.tasks.LocalGolem;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.function.BiFunction;

public class Tasks {
    public static void register(String id, BiFunction<MinecraftServer, StateSaverAndLoader.StoredData.currentPlayerQuestsEntry, Task> fromStringFactory) {
        taskTypes.add(new TaskType(id, fromStringFactory));
    }

    public record TaskType(String id,
                           BiFunction<MinecraftServer, StateSaverAndLoader.StoredData.currentPlayerQuestsEntry, Task> fromStringFactory) {
    }

    public static ArrayList<TaskType> taskTypes = new ArrayList<>();

    public static void savePlayerTask(Task task) {
        StateSaverAndLoader.StoredData.currentPlayerQuestsEntry oldEntry = task.entry;
        Main.state.data.currentPlayerQuests.get(oldEntry.playerUuid()).remove(oldEntry);
        StateSaverAndLoader.StoredData.currentPlayerQuestsEntry newEntry = new StateSaverAndLoader.StoredData.currentPlayerQuestsEntry(
                oldEntry.playerUuid(),
                oldEntry.villagerUuid(),
                oldEntry.taskType(),
                task.encode(),
                oldEntry.totalQuestProgress(),
                oldEntry.questUuid()
        );
        Main.state.data.currentPlayerQuests.get(oldEntry.playerUuid()).add(newEntry);
        task.entry = newEntry;
    }

    static {
        register(LocalGolem.id, LocalGolem::decode);
        register(GiveItem.id, GiveItem::decode);
    }

    public static Task decodeTask(MinecraftServer server, StateSaverAndLoader.StoredData.currentPlayerQuestsEntry entry) {
        for (var subEntry : taskTypes) {
            if (subEntry.id.equals(entry.taskType())) {
                return subEntry.fromStringFactory.apply(server, entry);
            }
        }
        return null;
    }

    public static Task getRandomTask(MinecraftServer server, ServerPlayerEntity player, VillagerEntity villager, int totalQuestProgress) {
        Random random = server.getOverworld().random;
        while (true) {
            if (random.nextInt(25) == 0) {
                Task task = LocalGolem.tryToCreate(server, player, villager, totalQuestProgress);
                if (task != null) return task;
            }
            if (random.nextBoolean()) {
                return GiveItem.create(server, player, villager, totalQuestProgress);
            }
        }
    }
}
