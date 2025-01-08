package jacksunderscoreusername.trinkets.quest;

import jacksunderscoreusername.trinkets.Main;
import jacksunderscoreusername.trinkets.StateSaverAndLoader;
import jacksunderscoreusername.trinkets.dialog.DialogHelper;
import jacksunderscoreusername.trinkets.dialog.DialogPage;
import jacksunderscoreusername.trinkets.dialog.DialogScreenHandler;
import jacksunderscoreusername.trinkets.minix_io.TrueVillager;
import jacksunderscoreusername.trinkets.trinkets.Trinket;
import jacksunderscoreusername.trinkets.trinkets.Trinkets;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.*;

import java.util.*;
import java.util.stream.Collectors;

public class QuestManager {

    public static final ScreenHandlerType<DialogScreenHandler> DIALOG_SCREEN_HANDLER = Registry.register(Registries.SCREEN_HANDLER, Identifier.of(Main.MOD_ID, "dialog_screen"), new ScreenHandlerType<>(DialogScreenHandler::new, FeatureSet.empty()));

    public static void finishTask(MinecraftServer server, StateSaverAndLoader.StoredData.currentPlayerQuestsEntry entry, int addedProgress) {
        Main.state.data.currentPlayerQuests.get(entry.playerUuid()).remove(entry);
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.playerUuid());
        assert player != null;
        currentPlayerTasks.get(player.getUuid()).remove(entry.villagerUuid());
        VillagerEntity villager = (VillagerEntity) player.getServerWorld().getEntity(entry.villagerUuid());
        assert villager != null;
        Optional<Pair<ItemStack, ItemStack>> reward = getReward(entry, entry.totalQuestProgress() + addedProgress);
        if (reward.isEmpty())
            startNewTask(player, villager, entry.totalQuestProgress() + addedProgress);
        else {
            DialogPage page = new DialogPage();
            page.addItems(new DialogPage.DialogPageItem(DialogPage.Type.TEXT).setText(Text.literal("Thanks for helping out, in thanks I've got something for you too").formatted(Formatting.BLACK)));
            page.setOpenCallback((subPlayer, inventory) -> {
                inventory.setStack(1, reward.get().getLeft());
                inventory.setStack(2, reward.get().getRight());
            });
            DialogHelper.openScreen(player, villager, page);
        }
    }

    public static void startNewTask(ServerPlayerEntity player, VillagerEntity villager, int totalQuestProgress) {
        Task task = Tasks.getRandomTask(player.server, player, villager, totalQuestProgress);
        if (!currentPlayerTasks.containsKey(player.getUuid()))
            currentPlayerTasks.put(player.getUuid(), new HashMap<>());
        currentPlayerTasks.get(player.getUuid()).put(villager.getUuid(), task);
        if (!Main.state.data.currentPlayerQuests.containsKey(player.getUuid()))
            Main.state.data.currentPlayerQuests.put(player.getUuid(), new ArrayList<>());
        Main.state.data.currentPlayerQuests.get(player.getUuid()).add(task.entry);
        DialogHelper.openScreen(player, villager, task.getPage());
    }

    public static HashMap<UUID, HashMap<UUID, Task>> currentPlayerTasks = new HashMap<>();

    private static final HashMap<Rarity, Integer> rarityMap = new HashMap<>();

    static {
        rarityMap.put(Rarity.UNCOMMON, 5);
        rarityMap.put(Rarity.RARE, 10);
        rarityMap.put(Rarity.EPIC, 15);
    }

    public static Optional<Pair<ItemStack, ItemStack>> getReward(StateSaverAndLoader.StoredData.currentPlayerQuestsEntry entry, int totalQuestProgress) {
        totalQuestProgress = totalQuestProgress * 2;
        Random random = new Random(entry.questUuid().hashCode() + totalQuestProgress);
        int maxQuestLength = 25;
        int minQuestLength = 5;
        if (totalQuestProgress < minQuestLength)
            return Optional.empty();
        if (totalQuestProgress >= maxQuestLength || random.nextInt(maxQuestLength) <= Math.ceilDiv(totalQuestProgress, 2)) {
            ItemStack firstItem = null;
            ItemStack secondItem = null;
            ArrayList<Trinket> allTrinkets = new ArrayList<>(List.of(Trinkets.AllTrinkets));
            ArrayList<Trinket> tempList = new ArrayList<>();
            while (!allTrinkets.isEmpty()) tempList.add(allTrinkets.remove(random.nextInt(allTrinkets.size())));
            allTrinkets = tempList;
            for (var trinket : allTrinkets.stream().sorted((a, b) -> rarityMap.get(b.getDefaultStack().getRarity()) - rarityMap.get(a.getDefaultStack().getRarity())).toList()) {
                if (Trinkets.canTrinketBeCreated(trinket.getId()) && rarityMap.get(trinket.getDefaultStack().getRarity()) <= totalQuestProgress) {
                    firstItem = trinket.getDefaultStack();
                    break;
                }
            }
            while (firstItem == null || secondItem == null) {
                Rarity rarity = List.of(Rarity.UNCOMMON, Rarity.RARE, Rarity.EPIC).get(random.nextInt(3));
                ItemStack item = switch (rarity) {
                    case COMMON -> null;
                    case UNCOMMON ->
                            Trinkets.UNCOMMON_TRINKET_DUST.getDefaultStack().copyWithCount(Math.ceilDiv(totalQuestProgress, rarityMap.get(rarity)));
                    case RARE ->
                            Trinkets.RARE_TRINKET_DUST.getDefaultStack().copyWithCount(Math.ceilDiv(totalQuestProgress, rarityMap.get(rarity)));
                    case EPIC ->
                            Trinkets.EPIC_TRINKET_DUST.getDefaultStack().copyWithCount(Math.ceilDiv(totalQuestProgress, rarityMap.get(rarity)));
                };
                if (firstItem == null) firstItem = item;
                else secondItem = item;
            }
            Main.LOGGER.info("Reward chosen for quest with value {} : {} {} and {} {}", totalQuestProgress, firstItem.getCount(), firstItem.getItem().getName().getLiteralString(), secondItem.getCount(), secondItem.getItem().getName().getLiteralString());
            return Optional.of(new Pair<>(firstItem, secondItem));
        }
        return Optional.empty();
    }

    public static void initialize() {
        UseEntityCallback.EVENT.register(((player, world, hand, entity, hitResult) -> {
            if (!world.isClient && entity instanceof VillagerEntity villager && !player.isSneaking() && player instanceof ServerPlayerEntity serverPlayer && hand.equals(Hand.MAIN_HAND)) {
                if (currentPlayerTasks.containsKey(player.getUuid()) && currentPlayerTasks.get(player.getUuid()).containsKey(villager.getUuid())) {
                    DialogHelper.openScreen(serverPlayer, villager, currentPlayerTasks.get(player.getUuid()).get(villager.getUuid()).getPage());
                    return ActionResult.SUCCESS;
                } else if (Main.state.data.currentPlayerQuests.containsKey(player.getUuid()) && Main.state.data.currentPlayerQuests.get(player.getUuid()).stream().anyMatch(entry -> entry.villagerUuid().equals(villager.getUuid()))) {
                    Task task = Tasks.decodeTask(serverPlayer.server, Main.state.data.currentPlayerQuests.get(player.getUuid()).stream().filter(entry -> entry.villagerUuid().equals(villager.getUuid())).findAny().get());
                    assert task != null;
                    if (!currentPlayerTasks.containsKey(player.getUuid()))
                        currentPlayerTasks.put(player.getUuid(), new HashMap<>());
                    currentPlayerTasks.get(player.getUuid()).put(villager.getUuid(), task);
                    DialogHelper.openScreen(serverPlayer, villager, task.getPage());
                    return ActionResult.SUCCESS;
                } else if (((TrueVillager) villager).trinkets_1_21_4_v2$canStartQuest(player)) {
                    startNewTask(serverPlayer, villager, 0);
                    return ActionResult.SUCCESS;
                }
            }
            return ActionResult.PASS;
        }));

        DialogHelper.initialize();
    }
}
