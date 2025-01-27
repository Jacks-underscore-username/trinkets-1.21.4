package jacksunderscoreusername.ancient_trinkets.quest.tasks;

import jacksunderscoreusername.ancient_trinkets.StateSaverAndLoader;
import jacksunderscoreusername.ancient_trinkets.dialog.DialogPage;
import jacksunderscoreusername.ancient_trinkets.quest.QuestManager;
import jacksunderscoreusername.ancient_trinkets.quest.Task;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.UUID;

public class LocalGolem extends Task {
    public static String id = "local_golem";

    public static LocalGolem tryToCreate(MinecraftServer server, ServerPlayerEntity player, VillagerEntity villager, int totalQuestProgress) {
        if (countNearbyGolems((ServerWorld) villager.getWorld(), villager.getBlockPos()) > 1)
            return null;
        LocalGolem instance = new LocalGolem();
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

    public static LocalGolem decode(MinecraftServer server, StateSaverAndLoader.StoredData.currentPlayerQuestsEntry entry) {
        LocalGolem instance = new LocalGolem();
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
        page.addItems(new DialogPage.DialogPageItem(DialogPage.Type.TEXT).setText(Text.literal("We could use another golem watching over us, could you get us 3?").formatted(Formatting.BLACK)));
        DialogPage.Callback callback = (subPlayer, inventory) -> {
            BlockPos pos = subPlayer.getBlockPos();
            ServerWorld world = (ServerWorld) subPlayer.getWorld();
            if (countNearbyGolems(world, pos) >= 3) {
                QuestManager.finishTask(subPlayer.server, this.entry, 1);
            }
        };
        page.addItems(new DialogPage.DialogPageItem(DialogPage.Type.BUTTON).setText(Text.literal("Done")).setAlignment(DialogPage.Alignment.BOTTOM).setClickCallback(callback));
        return page;
    }

    private static int countNearbyGolems(ServerWorld world, BlockPos pos) {
        return world.getEntitiesByType(EntityType.IRON_GOLEM, new Box(pos.getX() - 64, pos.getY() - 64, pos.getZ() - 64, pos.getX() + 64, pos.getY() + 64, pos.getZ() + 64), (entity) -> true).size();
    }
}
