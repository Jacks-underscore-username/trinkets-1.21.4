package jacksunderscoreusername.ancient_trinkets.quest.tasks;

import jacksunderscoreusername.ancient_trinkets.StateSaverAndLoader;
import jacksunderscoreusername.ancient_trinkets.dialog.DialogPage;
import jacksunderscoreusername.ancient_trinkets.quest.QuestManager;
import jacksunderscoreusername.ancient_trinkets.quest.Task;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;

import java.util.UUID;

public class BetweenTasks extends Task {
    public static String id = "between_tasks";

    public static BetweenTasks create(MinecraftServer server, ServerPlayerEntity player, VillagerEntity villager, int totalQuestProgress) {
        BetweenTasks instance = new BetweenTasks();
        instance.entry = new StateSaverAndLoader.StoredData.currentPlayerQuestsEntry(
                player.getUuid(),
                villager.getUuid(),
                id,
                "",
                totalQuestProgress,
                UUID.randomUUID()
        );
        return instance;
    }

    public static BetweenTasks decode(MinecraftServer server, StateSaverAndLoader.StoredData.currentPlayerQuestsEntry entry) {
        BetweenTasks instance = new BetweenTasks();
        instance.entry = entry;
        return instance;
    }

    @Override
    public String encode() {
        return "";
    }

    @Override
    public DialogPage getPage() {
        DialogPage page = new DialogPage();
        page.addItems(new DialogPage.DialogPageItem(DialogPage.Type.TEXT).setText(Text.literal("I have another job for you if you want").formatted(Formatting.BLACK)));
        Formatting color = Formatting.RED;
        int totalQuestProgress = entry.totalQuestProgress();
        if (totalQuestProgress >= QuestManager.rarityValueMap.get(Rarity.EPIC))
            color = Formatting.LIGHT_PURPLE;
        else if (totalQuestProgress >= QuestManager.rarityValueMap.get(Rarity.RARE))
            color = Formatting.AQUA;
        else if (totalQuestProgress >= QuestManager.rarityValueMap.get(Rarity.UNCOMMON))
            color = Formatting.YELLOW;
        page.addItems(new DialogPage.DialogPageItem(DialogPage.Type.TEXT).setText(Text.literal("Current quest value: " + totalQuestProgress).formatted(Formatting.ITALIC, color)));
        page.addItems(new DialogPage.DialogPageItem(DialogPage.Type.BUTTON)
                .setText(Text.literal("Accept"))
                .setClickCallback((subPlayer, inventory) -> {
                    QuestManager.finishTask(subPlayer.server, this.entry, false, false);
                })
                .setAlignment(DialogPage.Alignment.BOTTOM)
                .setForceLineBreak(false));
        page.addItems(new DialogPage.DialogPageItem(DialogPage.Type.BUTTON)
                .setText(Text.literal("Decline"))
                .setClickCallback((subPlayer, inventory) -> {
                    QuestManager.finishTask(subPlayer.server, this.entry, false, true);
                })
                .setAlignment(DialogPage.Alignment.BOTTOM));
        return page;
    }
}
