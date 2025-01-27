package jacksunderscoreusername.ancient_trinkets.quest.tasks;

import com.google.gson.Gson;
import jacksunderscoreusername.ancient_trinkets.StateSaverAndLoader;
import jacksunderscoreusername.ancient_trinkets.dialog.DialogPage;
import jacksunderscoreusername.ancient_trinkets.quest.QuestManager;
import jacksunderscoreusername.ancient_trinkets.quest.Task;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.random.Random;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KillMob extends Task {
    public static String id = "kill_mob";

    private static final Gson gson = new Gson();
    private EntityType<?> entityType;
    private int killCount;
    private int startKillCount;

    private record validMobEntry(int value, int maxNeeded, String message) {
    }

    private static final HashMap<EntityType<?>, validMobEntry> validMobs = new HashMap<>();

    static {
        validMobs.put(EntityType.ZOMBIE, new validMobEntry(5, 50, "the village has been being raided by hostile mobs, could you kill %s zombieS to help?"));
        validMobs.put(EntityType.SKELETON, new validMobEntry(5, 50, "the village has been being raided by hostile mobs, could you kill %s skeletonS to help?"));
        validMobs.put(EntityType.CREEPER, new validMobEntry(5, 50, "the village has been being raided by hostile mobs, could you kill %s creeperS to help?"));
        validMobs.put(EntityType.SPIDER, new validMobEntry(5, 50, "the village has been being raided by hostile mobs, could you kill %s spiderS to help?"));
        validMobs.put(EntityType.ENDERMAN, new validMobEntry(5, 25, "the village has been being raided by hostile mobs, could you kill %s enderman to help?"));
        validMobs.put(EntityType.WITCH, new validMobEntry(5, 5, "the village has been being raided by hostile mobs, could you kill %s witchES to help?"));
        validMobs.put(EntityType.EVOKER, new validMobEntry(10, 3, "the village has been being raided by hostile mobs, could you kill %s evokerS to help?"));
        validMobs.put(EntityType.VINDICATOR, new validMobEntry(9, 3, "the village has been being raided by hostile mobs, could you kill %s vindicatorS to help?"));
        validMobs.put(EntityType.PILLAGER, new validMobEntry(5, 5, "the village has been being raided by hostile mobs, could you kill %s pillagerS to help?"));
        validMobs.put(EntityType.VEX, new validMobEntry(9, 10, "the village has been being raided by hostile mobs, could you kill %s vexES to help?"));
        validMobs.put(EntityType.PHANTOM, new validMobEntry(5, 15, "the village has been being raided by hostile mobs, could you kill %s phantomS to help?"));
    }

    private record storedData(String type, int count, int startCount) {
    }

    public static KillMob create(MinecraftServer server, ServerPlayerEntity player, VillagerEntity villager, int totalQuestProgress) {
        Random random = server.getOverworld().random;
        KillMob instance = new KillMob();

        Map.Entry<EntityType<?>, validMobEntry> entry = validMobs.entrySet().stream().toList().get(random.nextInt(validMobs.size()));
        instance.entityType = entry.getKey();
        instance.killCount = random.nextInt(entry.getValue().maxNeeded) + 1;
        instance.startKillCount = player.getStatHandler().getStat(Stats.KILLED.getOrCreateStat(instance.entityType));

        instance.entry = new StateSaverAndLoader.StoredData.currentPlayerQuestsEntry(
                player.getUuid(),
                villager.getUuid(),
                id,
                gson.toJson(new storedData(EntityType.getId(instance.entityType).toString(), instance.killCount, instance.startKillCount)),
                totalQuestProgress,
                UUID.randomUUID()
        );
        return instance;
    }

    public static KillMob decode(MinecraftServer server, StateSaverAndLoader.StoredData.currentPlayerQuestsEntry entry) {
        KillMob instance = new KillMob();
        instance.entry = entry;
        storedData data = gson.fromJson(entry.encodedTask(), storedData.class);
        instance.entityType = EntityType.get(data.type).get();
        instance.killCount = data.count;
        return instance;
    }

    @Override
    public String encode() {
        return gson.toJson(new storedData(EntityType.getId(entityType).toString(), killCount, startKillCount));
    }

    @Override
    public DialogPage getPage() {
        DialogPage page = new DialogPage();
        String text = validMobs.get(entityType).message.replaceAll("%s", String.valueOf(killCount)).toLowerCase();
        if (killCount == 1)
            text = text.replaceAll("[A-Z]", "");
        else
            text = text.toLowerCase();
        text = text.substring(0, 1).toUpperCase() + text.substring(1);
        page.addItems(new DialogPage.DialogPageItem(DialogPage.Type.TEXT).setText(Text.literal(text).formatted(Formatting.BLACK)));
        DialogPage.Callback callback = (subPlayer, inventory) -> {
            int killedCount = subPlayer.getStatHandler().getStat(Stats.KILLED.getOrCreateStat(entityType));
            if (killedCount - startKillCount >= killCount) {
                validMobEntry entry = validMobs.get(entityType);
                QuestManager.finishTask(subPlayer.server, this.entry, (int) Math.ceil(entry.value * killCount / ((float) entry.maxNeeded)));
            }
        };
        page.addItems(new DialogPage.DialogPageItem(DialogPage.Type.BUTTON).setText(Text.literal("Done")).setAlignment(DialogPage.Alignment.BOTTOM).setClickCallback(callback));
        return page;
    }
}
