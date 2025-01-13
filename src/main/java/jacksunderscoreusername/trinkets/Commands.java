package jacksunderscoreusername.trinkets;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import jacksunderscoreusername.trinkets.trinkets.CooldownDataComponent;
import jacksunderscoreusername.trinkets.trinkets.Trinket;
import jacksunderscoreusername.trinkets.trinkets.TrinketDataComponent;
import jacksunderscoreusername.trinkets.trinkets.Trinkets;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;

import java.util.*;

public class Commands {
    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("upgradeTrinket")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            if (player == null) {
                                return 0;
                            }
                            ItemStack stack = player.getMainHandStack();
                            if (!(stack.getItem() instanceof Trinket)) {
                                context.getSource().sendFeedback(() -> Text.literal("You have to be holding a trinket to upgrade it"), false);
                                return 0;
                            }
                            TrinketDataComponent.TrinketData oldData = stack.get(TrinketDataComponent.TRINKET_DATA);
                            stack.set(TrinketDataComponent.TRINKET_DATA, new TrinketDataComponent.TrinketData(oldData.level() + 1, oldData.UUID(), oldData.interference(), oldData.trackerCount()));
                            context.getSource().sendFeedback(() -> Text.literal("Upgraded trinket (now level " + (oldData.level() + 1) + ")"), true);
                            return 1;
                        })));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("listTrinkets")
                        .executes(context -> {
                            int openCount = 0;
                            HashMap<Rarity, Integer> rarityMap = new HashMap<>();
                            rarityMap.put(Rarity.UNCOMMON, 3);
                            rarityMap.put(Rarity.RARE, 2);
                            rarityMap.put(Rarity.EPIC, 1);
                            for (var trinket : Trinkets.allTrinkets.stream().sorted(Comparator.comparingInt(trinket -> rarityMap.get(trinket.getDefaultStack().getRarity()))).toList()) {
                                boolean taken = !Trinkets.canTrinketBeCreated(trinket.getId());
                                if (!taken)
                                    openCount++;
                                Formatting color = Trinkets.getTrinketColor(trinket);
                                Text message = Text.literal(trinket.getDisplayName()).formatted(color).append(Text.literal(" : ").formatted(Formatting.WHITE)).append(Text.literal(taken ? "Claimed" : "Open").formatted(taken ? Formatting.RED : Formatting.GREEN));
                                context.getSource().sendFeedback(() -> message, false);
                            }
                            int finalOpenCount = openCount;
                            context.getSource().sendFeedback(() -> Text.literal(finalOpenCount + "/" + Trinkets.allTrinkets.size() + " trinkets can still be claimed"), false);
                            return 1;
                        })));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("resetCooldown")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            if (player == null) {
                                return 0;
                            }
                            ItemStack stack = player.getMainHandStack();
                            if (!(stack.getItem() instanceof Trinket)) {
                                context.getSource().sendFeedback(() -> Text.literal("You have to be holding a trinket reset it's cooldown"), false);
                                return 0;
                            }
                            if (stack.contains(CooldownDataComponent.COOLDOWN))
                                stack.remove(CooldownDataComponent.COOLDOWN);
                            else {
                                context.getSource().sendFeedback(() -> Text.literal("Selected trinket has no cooldown"), false);
                                return 0;
                            }
                            context.getSource().sendFeedback(() -> Text.literal("Reset trinket cooldown"), true);
                            return 1;
                        })));

//        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
//                dispatcher.register(CommandManager.literal("locateTrinket")
//                        .requires(source -> source.hasPermissionLevel(2))
//                        .then(CommandManager.argument("player", StringArgumentType.string())
//                                .suggests((context, builder) -> CommandSource.suggestMatching(Main.state.data.claimedTrinketPlayerMap.values().stream().map(uuid -> Main.server.getUserCache().getByUuid(uuid).get().getName()).toList(), builder))
//                                .executes(context -> {
//                                    UUID playerUuid = Main.server.getUserCache().findByName(StringArgumentType.getString(context, "player")).get().getId();
//                                    List<UUID> playerTrinkets = Main.state.data.claimedTrinketPlayerMap.entrySet().stream().filter(entry -> entry.getValue().equals(playerUuid)).map(Map.Entry::getKey).toList();
//                                    for (var uuid : playerTrinkets) {
//                                        StateSaverAndLoader.StoredData.lastTrinketLocationEntry lastEntry = Main.state.data.lastTrinketLocations.get(uuid);
//                                        MutableText message = Text.literal(uuid.toString() + " was last seen at " + lastEntry.pos.toString() + " " + Utils.prettyTime((int) ((System.currentTimeMillis() - lastEntry.time) / 1000), true) + " ago");
//                                        context.getSource().sendFeedback(() -> message, false);
//                                    }
//                                    return 1;
//                                }))));
//
    }
}
