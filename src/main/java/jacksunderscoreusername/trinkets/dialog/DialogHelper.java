package jacksunderscoreusername.trinkets.dialog;

import jacksunderscoreusername.trinkets.payloads.CloseDialogPagePayload;
import jacksunderscoreusername.trinkets.payloads.DialogClickedPayload;
import jacksunderscoreusername.trinkets.payloads.SendDialogPagePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class DialogHelper {
    public static HashMap<UUID, List<DialogPage.Callback>> buttonCallbacks = new HashMap<>();

    public static HashMap<UUID, Inventory> screenInventories = new HashMap<>();

    public static void openScreen(ServerPlayerEntity player, LivingEntity speakingEntity, DialogPage page) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, playerInventory, subPlayer) ->
                new DialogScreenHandler(syncId, playerInventory, speakingEntity), speakingEntity.getDisplayName()));
        ServerPlayNetworking.send(player, new SendDialogPagePayload(page.build(player)));
        if (page.callback != null)
            page.callback.apply(player, (SimpleInventory) screenInventories.get(player.getUuid()));
    }

    public static void closeScreen(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, new CloseDialogPagePayload(true));
    }

    public static void initialize() {
        ServerPlayNetworking.registerGlobalReceiver(DialogClickedPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                DialogHelper.buttonCallbacks.get(context.player().getUuid()).get(payload.index()).apply(context.player(), (SimpleInventory) screenInventories.get(context.player().getUuid()));
            });
        });
    }
}
