package jacksunderscoreusername.trinkets.quest.tasks;

import com.google.gson.Gson;
import jacksunderscoreusername.trinkets.StateSaverAndLoader;
import jacksunderscoreusername.trinkets.dialog.DialogPage;
import jacksunderscoreusername.trinkets.quest.QuestManager;
import jacksunderscoreusername.trinkets.quest.Task;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.random.Random;

import java.util.HashMap;
import java.util.UUID;

public class GiveItem extends Task {
    public static String id = "give_item";

    private static final Gson gson = new Gson();
    private ItemStack cost = null;

    private record validItem(int value, int max, String message) {
    }

    private static final HashMap<Item, validItem> validItems = new HashMap<>();

    static {
        validItems.put(Items.IRON_BLOCK, new validItem(3, 16, "i've been running low on some stuff, could you bring me %s iron blockS?"));
        validItems.put(Items.GOLD_BLOCK, new validItem(3, 8, "i've been running low on some stuff, could you bring me %s gold blockS?"));
        validItems.put(Items.EMERALD, new validItem(3, 64, "the market's been tough, could you bring me %s emeraldS?"));
        validItems.put(Items.DIAMOND, new validItem(5, 16, "i've been running low on some stuff, could you bring me %s diamondS?"));
        validItems.put(Items.LAPIS_LAZULI, new validItem(3, 64, "i've been running low on some stuff, could you bring me %s lapis?"));
        validItems.put(Items.QUARTZ, new validItem(2, 64, "i've been running low on some stuff, could you bring me %s quartz?"));
        validItems.put(Items.BREAD, new validItem(2, 64, "my food's running low, could you bring me %s bread?"));
        validItems.put(Items.SCULK_SHRIEKER, new validItem(10, 10, "i've heard about some new rare item, could you bring me %s sculk shriekerS?"));
        validItems.put(Items.WITHER_SKELETON_SKULL, new validItem(10, 1, "i've heard about some new rare item, could you bring me %s wither skeleton skullS?"));
        validItems.put(Items.GHAST_TEAR, new validItem(5, 5, "i've heard about some new rare item, could you bring me %s ghast tearS?"));
        validItems.put(Items.PRISMARINE_SHARD, new validItem(10, 10, "i've heard about some new rare item, could you bring me %s prismarine shardS?"));
        validItems.put(Items.ENCHANTING_TABLE, new validItem(5, 1, "i've been wanting to improve my house, could you bring me %s enchanting tableS?"));
        validItems.put(Items.NETHER_STAR, new validItem(13, 1, "i've heard about some new rare item, could you bring me %s nether starS?"));
        validItems.put(Items.ENDER_EYE, new validItem(5, 16, "i've heard about some new rare item, could you bring me %s ender eyeS?"));
        validItems.put(Items.DRAGON_BREATH, new validItem(10, 16, "i've heard about some new rare item, could you bring me %s dragon breath?"));
        validItems.put(Items.DRAGON_HEAD, new validItem(10, 1, "i've heard about some new rare item, could you bring me %s dragon headS?"));
        validItems.put(Items.ENDER_PEARL, new validItem(4, 16, "i need a faster way of traveling, could you bring me %s ender pearlS?"));
        validItems.put(Items.ELYTRA, new validItem(13, 1, "i need a faster way of traveling, could you bring me %s elytra?"));
        validItems.put(Items.RECOVERY_COMPASS, new validItem(10, 1, "i've need some protection, could you bring me %s recovery compassES?"));
        validItems.put(Items.MACE, new validItem(13, 1, "i've need some protection, could you bring me %s maceS?"));
        validItems.put(Items.TOTEM_OF_UNDYING, new validItem(8, 1, "i've need some protection, could you bring me %s totemS of undying?"));
        validItems.put(Items.RESIN_CLUMP, new validItem(5, 32, "i've heard about some new rare item, could you bring me %s resin clumpS?"));
        validItems.put(Items.ECHO_SHARD, new validItem(5, 8, "i've heard about some new rare item, could you bring me %s echo shardS?"));
        validItems.put(Items.ENDER_CHEST, new validItem(3, 5, "i've heard about some new rare item, could you bring me %s ender chestS?"));
        validItems.put(Items.RABBIT_FOOT, new validItem(3, 16, "i've heard about some new rare item, could you bring me %s rabbit feet?"));
        validItems.put(Items.NETHER_WART, new validItem(3, 16, "i've heard about some new rare item, could you bring me %s nether wartS?"));
        validItems.put(Items.OMINOUS_TRIAL_KEY, new validItem(15, 5, "i've heard about some new rare item, could you bring me %s ominous trial keyS?"));
        validItems.put(Items.TRIAL_KEY, new validItem(10, 10, "i've heard about some new rare item, could you bring me %s trial keyS?"));
        validItems.put(Items.EXPERIENCE_BOTTLE, new validItem(3, 5, "i've heard about some new rare item, could you bring me %s experience bottleS?"));
        validItems.put(Items.CREAKING_HEART, new validItem(5, 5, "i've heard about some new rare item, could you bring me %s creaking heartS?"));
        validItems.put(Items.HEART_OF_THE_SEA, new validItem(5, 1, "i've heard about some new rare item, could you bring me %s heartS of the sea?"));
        validItems.put(Items.ANCIENT_DEBRIS, new validItem(5, 16, "i've heard about some new rare item, could you bring me %s ancient debris?"));
        validItems.put(Items.AMETHYST_SHARD, new validItem(5, 32, "i've heard about some new rare item, could you bring me %s amethyst shardS?"));
        validItems.put(Items.SHULKER_SHELL, new validItem(8, 16, "i've heard about some new rare item, could you bring me %s shulker shellS?"));
        validItems.put(Items.BLAZE_ROD, new validItem(3, 16, "i've heard about some new rare item, could you bring me %s blaze rodS?"));
        validItems.put(Items.GOLDEN_APPLE, new validItem(3, 8, "my food's running low, could you bring me %s golden appleS?"));
        validItems.put(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, new validItem(5, 1, "i've need some protection, could you bring me %s netherite upgrade templateS?"));
        validItems.put(Items.GLOW_INK_SAC, new validItem(3, 16, "i've heard about some new rare item, could you bring me %s glow ink sacS?"));
        validItems.put(Items.ARMADILLO_SCUTE, new validItem(3, 16, "i've heard about some new rare item, could you bring me %s armadillo scuteS?"));
        validItems.put(Items.TURTLE_SCUTE, new validItem(5, 5, "i've heard about some new rare item, could you bring me %s turtle scuteS?"));
        validItems.put(Items.PHANTOM_MEMBRANE, new validItem(3, 16, "i've heard about some new rare item, could you bring me %s phantom membraneS?"));
    }

    private record storedData(int id, int count) {
    }

    public static GiveItem create(MinecraftServer server, ServerPlayerEntity player, VillagerEntity villager, int totalQuestProgress) {
        Random random = server.getOverworld().random;
        GiveItem instance = new GiveItem();

        Item item = validItems.keySet().stream().toList().get(random.nextInt(validItems.size()));
        instance.cost = item.getDefaultStack().copyWithCount(random.nextInt(validItems.get(item).max) + 1);

        instance.entry = new StateSaverAndLoader.StoredData.currentPlayerQuestsEntry(
                player.getUuid(),
                villager.getUuid(),
                id,
                gson.toJson(new storedData(Item.getRawId(item), instance.cost.getCount())),
                totalQuestProgress,
                UUID.randomUUID()
        );
        return instance;
    }

    public static GiveItem decode(MinecraftServer server, StateSaverAndLoader.StoredData.currentPlayerQuestsEntry entry) {
        GiveItem instance = new GiveItem();
        instance.entry = entry;
        storedData data = gson.fromJson(entry.encodedTask(), storedData.class);
        instance.cost = Item.byRawId(data.id).getDefaultStack().copyWithCount(data.count);
        return instance;
    }

    @Override
    public String encode() {
        return gson.toJson(new storedData(Item.getRawId(cost.getItem()), cost.getCount()));
    }

    @Override
    public DialogPage getPage() {
        DialogPage page = new DialogPage();
        String text = validItems.get(cost.getItem()).message.replaceAll("%s", String.valueOf(cost.getCount()));
        if (cost.getCount() == 1)
            text = text.replaceAll("[A-Z]", "");
        else
            text = text.toLowerCase();
        text = text.substring(0, 1).toUpperCase() + text.substring(1);
        page.addItems(new DialogPage.DialogPageItem(DialogPage.Type.TEXT).setText(Text.literal(text).formatted(Formatting.BLACK)));
        DialogPage.Callback callback = (subPlayer, inventory) -> {
            if (inventory.getStack(0).isOf(cost.getItem()) && inventory.getStack(0).getCount() >= cost.getCount()) {
                inventory.getStack(0).decrement(cost.getCount());
                validItem entry = validItems.get(cost.getItem());
                QuestManager.finishTask(subPlayer.server, this.entry, (int) Math.ceil(entry.value * cost.getCount() / ((float)entry.max)));
            }
        };
        page.addItems(new DialogPage.DialogPageItem(DialogPage.Type.BUTTON).setText(Text.literal("Done")).setAlignment(DialogPage.Alignment.BOTTOM).setClickCallback(callback));
        return page;
    }
}
