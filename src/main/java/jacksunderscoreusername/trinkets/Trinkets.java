package jacksunderscoreusername.trinkets;

import com.google.common.collect.ImmutableList;
import jacksunderscoreusername.trinkets.trinkets.activated_echo_shard.ActivatedEchoShard;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

import java.util.*;
import java.util.function.Function;

public class Trinkets {
    public static final HashMap<Rarity, Formatting> rarityColors = new HashMap<>();

    public static final Trinket ACTIVATED_ECHO_SHARD = register(ActivatedEchoShard.id, ActivatedEchoShard::new, ActivatedEchoShard.getSettings());
//    public static final Trinket SHULKER_SWARM = register(ShulkerSwarm.id, ShulkerSwarm::new, ShulkerSwarm.getSettings());

    public static final Trinket[] AllTrinkets = {ACTIVATED_ECHO_SHARD};

    public static Trinket register(String id, Function<Item.Settings, Item> factory, Item.Settings settings) {

        final RegistryKey<Item> registryKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Main.MOD_ID, id));
        return (Trinket) Items.register(registryKey, factory, settings);
    }

    public static boolean canTrinketBeCreated(String id) {
        return Main.state.data.createdTrinkets.getOrDefault(id, 0) < getTrinketLimit(id);
    }

    // I have it as a function here so that later different trinkets can have different limits.
    public static int getTrinketLimit(String id) {
        return Main.config.max_trinket_count == 0 ? Integer.MAX_VALUE : Main.config.max_trinket_count;
    }

    // I have it as a function here so that later different trinkets can have different limits.
    public static int getMaxDurability(String id) {
        return Main.config.max_uses;
    }

    public static boolean canPlayerUseTrinkets(PlayerEntity player) {
        if (Main.config.max_player_trinkets == 0) {
            return true;
        }
        if (Main.config.player_limit_mode == 1) {
            if (!Main.state.data.playerTrinketUseHistory.containsKey(player.getUuid())) {
                return true;
            }
            int trinketCount = 0;
            int now = Main.server.getTicks();
            ArrayList<UUID> alreadyCountedUuids = new ArrayList<>();
            for (var entry : Main.state.data.playerTrinketUseHistory.get(player.getUuid())) {
                if (!alreadyCountedUuids.contains(entry.itemUuid) && (now - entry.time) / 20 <= Main.config.trinket_interference_cooldown) {
                    alreadyCountedUuids.add(entry.itemUuid);
                    trinketCount++;
                }
            }
            return trinketCount <= Main.config.max_player_trinkets;
        }
        if (Main.config.player_limit_mode == 2) {
            int count = 0;
            for (var entry : Main.state.data.claimedTrinketPlayerMap.entrySet()) {
                if (entry.getValue().equals(player.getUuid())) {
                    count++;
                }
            }
            return count <= Main.config.max_player_trinkets;
        }
        throw new RuntimeException("Invalid something I'm sure");
    }

    public static void initialize() {
        rarityColors.put(Rarity.COMMON, Formatting.WHITE);
        rarityColors.put(Rarity.UNCOMMON, Formatting.YELLOW);
        rarityColors.put(Rarity.RARE, Formatting.AQUA);
        rarityColors.put(Rarity.EPIC, Formatting.LIGHT_PURPLE);

        ServerTickEvents.START_WORLD_TICK.register((world) -> {
            if (world.isClient) {
                return;
            }
            int now = Main.server.getTicks();
            ArrayList<UUID> currentTrinketPlayerMapKeysToRemove = new ArrayList<>();
            for (var entry : Main.state.data.currentTrinketPlayerMap.entrySet()) {
                ServerPlayerEntity player = Main.server.getPlayerManager().getPlayer(entry.getValue().player);
                if (player == null || !player.getInventory().contains((item) -> {
                    TrinketDataComponent.TrinketData data = item.getOrDefault(TrinketDataComponent.TRINKET_DATA, null);
                    return data != null && data.UUID().equals(entry.getKey().toString());
                })) {
                    currentTrinketPlayerMapKeysToRemove.add(entry.getKey());
                }
            }
            for (var key : currentTrinketPlayerMapKeysToRemove) {
                Main.state.data.currentTrinketPlayerMap.remove(key);
            }
            for (var entry : Main.state.data.playerTrinketUseHistory.entrySet()) {
                entry.getValue().removeIf(subEntry -> (subEntry.time - now) / 20 > Main.config.trinket_interference_cooldown);
            }
            for (var player : Main.server.getPlayerManager().getPlayerList()) {
                boolean state = Trinkets.canPlayerUseTrinkets(player);
                PlayerInventory inventory = player.getInventory();
                for (List<ItemStack> list : ImmutableList.of(inventory.main, inventory.armor, inventory.offHand)) {
                    for (var item : list) {
                        TrinketDataComponent.TrinketData data = item.get(TrinketDataComponent.TRINKET_DATA);
                        if (data == null) {
                            continue;
                        }
                        if (!state && data.interference() == 0) {
                            item.set(TrinketDataComponent.TRINKET_DATA, new TrinketDataComponent.TrinketData(data.level(), data.UUID(), 1));
                        }
                        if (state && data.interference() == 1) {
                            item.set(TrinketDataComponent.TRINKET_DATA, new TrinketDataComponent.TrinketData(data.level(), data.UUID(), 0));
                        }
                    }
                }
            }
        });

        TrinketsItemGroup.initialize();
        for (var trinket : AllTrinkets) {
            trinket.initialize();
        }
    }
}
