package jacksunderscoreusername.trinkets;

import jacksunderscoreusername.trinkets.trinkets.Trinket;
import jacksunderscoreusername.trinkets.trinkets.TrinketDataComponent;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

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
                            stack.set(TrinketDataComponent.TRINKET_DATA, new TrinketDataComponent.TrinketData(oldData.level() + 1, oldData.UUID(), oldData.interference()));
                            context.getSource().sendFeedback(() -> Text.literal("Upgraded trinket (now level " + (oldData.level() + 1) + ")"), true);
                            return 1;
                        })));
    }
}
