package jacksunderscoreusername.trinkets.trinkets;

import com.google.common.collect.ImmutableList;
import jacksunderscoreusername.trinkets.Main;
import jacksunderscoreusername.trinkets.trinkets.breeze_core.BreezeCore;
import jacksunderscoreusername.trinkets.trinkets.fire_wand.FireWand;
import jacksunderscoreusername.trinkets.trinkets.original_totem.OriginalTotem;
import jacksunderscoreusername.trinkets.trinkets.soul_lamp.SoulLamp;
import jacksunderscoreusername.trinkets.trinkets.dragons_fury.DragonsFury;
import jacksunderscoreusername.trinkets.trinkets.eternal_bonemeal.EternalBonemeal;
import jacksunderscoreusername.trinkets.trinkets.gravity_disruptor.GravityDisruptor;
import jacksunderscoreusername.trinkets.trinkets.activated_echo_shard.ActivatedEchoShard;
import jacksunderscoreusername.trinkets.trinkets.suspicious_substance.SuspiciousSubstance;
import jacksunderscoreusername.trinkets.trinkets.trinket_dust.EpicTrinketDust;
import jacksunderscoreusername.trinkets.trinkets.trinket_dust.RareTrinketDust;
import jacksunderscoreusername.trinkets.trinkets.trinket_dust.UncommonTrinketDust;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.*;

import java.util.*;
import java.util.function.Function;

public class Trinkets {
    public static final HashMap<Rarity, Formatting> rarityColors = new HashMap<>();

    public static final ArrayList<Trinket> allTrinkets = new ArrayList<>();

    public static final Trinket ACTIVATED_ECHO_SHARD = register(ActivatedEchoShard.id, ActivatedEchoShard::new, ActivatedEchoShard.getSettings());
    public static final Trinket GRAVITY_DISRUPTOR = register(GravityDisruptor.id, GravityDisruptor::new, GravityDisruptor.getSettings());
    public static final Trinket DRAGONS_FURY = register(DragonsFury.id, DragonsFury::new, DragonsFury.getSettings());
    public static final Trinket ETERNAL_BONEMEAL = register(EternalBonemeal.id, EternalBonemeal::new, EternalBonemeal.getSettings());
    public static final Trinket SUSPICIOUS_SUBSTANCE = register(SuspiciousSubstance.id, SuspiciousSubstance::new, SuspiciousSubstance.getSettings());
    public static final Trinket SOUL_LAMP = register(SoulLamp.id, SoulLamp::new, SoulLamp.getSettings());
    public static final Trinket FIRE_WAND = register(FireWand.id, FireWand::new, FireWand.getSettings());
    public static final Trinket BREEZE_CORE = register(BreezeCore.id, BreezeCore::new, BreezeCore.getSettings());
    public static final Trinket ORIGINAL_TOTEM = register(OriginalTotem.id, OriginalTotem::new, OriginalTotem.getSettings());

    public static final Item UNCOMMON_TRINKET_DUST = Items.register(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Main.MOD_ID, UncommonTrinketDust.id)), UncommonTrinketDust::new, UncommonTrinketDust.getSettings());
    public static final Item RARE_TRINKET_DUST = Items.register(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Main.MOD_ID, RareTrinketDust.id)), RareTrinketDust::new, RareTrinketDust.getSettings());
    public static final Item EPIC_TRINKET_DUST = Items.register(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Main.MOD_ID, EpicTrinketDust.id)), EpicTrinketDust::new, EpicTrinketDust.getSettings());

    public static Trinket register(String id, Function<Item.Settings, Item> factory, Item.Settings settings) {
        final RegistryKey<Item> registryKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Main.MOD_ID, id));
        Trinket trinket = (Trinket) Items.register(registryKey, factory, settings);
        allTrinkets.add(trinket);
        return trinket;
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

    public static Formatting getTrinketColor(Trinket trinket) {
        return switch (trinket.getDefaultStack().getRarity()) {
            case Rarity.EPIC -> Formatting.LIGHT_PURPLE;
            case Rarity.RARE -> Formatting.AQUA;
            case Rarity.UNCOMMON -> Formatting.YELLOW;
            default -> throw new RuntimeException("Invalid trinket rarity");
        };
    }

    public static void initialize() {
        rarityColors.put(Rarity.COMMON, Formatting.WHITE);
        rarityColors.put(Rarity.UNCOMMON, Formatting.YELLOW);
        rarityColors.put(Rarity.RARE, Formatting.AQUA);
        rarityColors.put(Rarity.EPIC, Formatting.LIGHT_PURPLE);

        CooldownDataComponent.initialize();
        ChargesDataComponent.initialize();
        TrinketCompassDataComponent.initialize();
        AbstractModeDataComponent.initialize();
        TrinketsItemGroup.initialize();
        for (var trinket : allTrinkets) {
            trinket.initialize();
        }

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
                            item.set(TrinketDataComponent.TRINKET_DATA, new TrinketDataComponent.TrinketData(data.level(), data.UUID(), 1, data.trackerCount()));
                        }
                        if (state && data.interference() == 1) {
                            item.set(TrinketDataComponent.TRINKET_DATA, new TrinketDataComponent.TrinketData(data.level(), data.UUID(), 0, data.trackerCount()));
                        }

                        CooldownDataComponent.CooldownData cooldownData = item.get(CooldownDataComponent.COOLDOWN);
                        if (cooldownData != null) {
                            if (cooldownData.startTime() > now) {
                                item.set(CooldownDataComponent.COOLDOWN, new CooldownDataComponent.CooldownData(0, cooldownData.totalTime(), cooldownData.totalTime()));
                                continue;
                            }
                            int timeLeft = cooldownData.totalTime() - (now - cooldownData.startTime()) / 20;
                            if (item.getItem() instanceof TrinketWithCharges itemClass && cooldownData.totalTime() > itemClass.getChargeTime(item))
                                item.set(CooldownDataComponent.COOLDOWN, new CooldownDataComponent.CooldownData(cooldownData.startTime(), cooldownData.timeLeft(), itemClass.getChargeTime(item)));
                            if (timeLeft <= 0) {
                                item.remove(CooldownDataComponent.COOLDOWN);
                            } else if (timeLeft != cooldownData.timeLeft()) {
                                item.set(CooldownDataComponent.COOLDOWN, new CooldownDataComponent.CooldownData(cooldownData.startTime(), cooldownData.totalTime(), timeLeft));
                            }
                        }
                        if (cooldownData == null && item.getItem() instanceof TrinketWithCharges itemClass) {
                            int charges = (item.get(ChargesDataComponent.CHARGES) == null ? 0 : Objects.requireNonNull(item.get(ChargesDataComponent.CHARGES)).charges()) + 1;
                            if (itemClass.getMaxCharges(item) >= charges) {
                                item.set(ChargesDataComponent.CHARGES, new ChargesDataComponent.Charges(charges));
                                item.set(CooldownDataComponent.COOLDOWN, new CooldownDataComponent.CooldownData(now, itemClass.getChargeTime(item), itemClass.getChargeTime(item)));
                            }
                        }
                    }
                }
            }
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!hand.equals(Hand.MAIN_HAND)) return ActionResult.PASS;
            ItemStack compass = player.getMainHandStack();
            if (compass.isEmpty() || !compass.isOf(Items.COMPASS)) return ActionResult.PASS;
            ItemStack trinket = player.getOffHandStack();
            if (trinket.isEmpty() || !(trinket.getItem() instanceof Trinket)) return ActionResult.PASS;
            TrinketDataComponent.TrinketData trinketData = trinket.get(TrinketDataComponent.TRINKET_DATA);
            if (trinketData == null || trinketData.UUID().length() <= 1) return ActionResult.PASS;
            if (!world.isClient) {
                TrinketCompassDataComponent.TrinketCompassData oldData = compass.get(TrinketCompassDataComponent.TRINKET_COMPASS);
                HashMap<UUID, Integer> compassCounts = Main.state.data.trinketCompasses;
                if (oldData != null)
                    compassCounts.put(UUID.fromString(oldData.trinketUuid()), compassCounts.get(UUID.fromString(oldData.trinketUuid())) - compass.getCount());
                compassCounts.put(UUID.fromString(trinketData.UUID()), compassCounts.getOrDefault(UUID.fromString(trinketData.UUID()), 0) + compass.getCount());
            }
            compass.set(TrinketCompassDataComponent.TRINKET_COMPASS, new TrinketCompassDataComponent.TrinketCompassData(trinketData.UUID(), ((Trinket) trinket.getItem()).getDisplayName() + " Tracker"));
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1, 1);
            return ActionResult.SUCCESS;
        });
    }
}
