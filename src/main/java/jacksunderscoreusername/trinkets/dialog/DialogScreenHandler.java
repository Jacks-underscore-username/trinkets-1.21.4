package jacksunderscoreusername.trinkets.dialog;

import jacksunderscoreusername.trinkets.Main;
import jacksunderscoreusername.trinkets.quest.QuestManager;
import jacksunderscoreusername.trinkets.payloads.SetDialogEntityPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Objects;
import java.util.UUID;

public class DialogScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final LivingEntity speakingEntity;

    public DialogScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, playerInventory.player);
    }

    public DialogScreenHandler(int syncId, PlayerInventory playerInventory, LivingEntity speakingEntity) {
        this(syncId, playerInventory, speakingEntity, new SimpleInventory(3));
    }

    public DialogScreenHandler(int syncId, PlayerInventory playerInventory, LivingEntity speakingEntity, Inventory inventory) {
        super(QuestManager.DIALOG_SCREEN_HANDLER, syncId);
        boolean isClient = speakingEntity.getWorld().isClient;
        if (!isClient) {
            UUID key = playerInventory.player.getUuid();
            DialogHelper.currentCallbacks.remove(key);
            if (DialogHelper.newCallbacks.containsKey(key)) {
                DialogHelper.currentCallbacks.put(key, DialogHelper.newCallbacks.get(key));
                DialogHelper.newCallbacks.remove(key);
            }

        }
        this.inventory = inventory;
        this.speakingEntity = speakingEntity;
        if (!isClient) {
            DialogHelper.screenInventories.put(playerInventory.player.getUuid(), inventory);
        }
        inventory.onOpen(playerInventory.player);

        this.addSlot(new Slot(inventory, 0, 8, 8));
        this.addSlot(new OutputSlot(inventory, 1, 8, 8 + 18 * 2));
        this.addSlot(new OutputSlot(inventory, 2, 8, 8 + 18 * 3));

        int m;
        int l;
        for (m = 0; m < 3; ++m) {
            for (l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + m * 9 + 9, 8 + l * 18, 84 + m * 18));
            }
        }
        for (m = 0; m < 9; ++m) {
            this.addSlot(new Slot(playerInventory, m, 8 + m * 18, 142));
        }

        PlayerEntity player = playerInventory.player;
        if (!isClient) {
            ServerPlayNetworking.send(Objects.requireNonNull(Main.server.getPlayerManager().getPlayer(player.getUuid())), new SetDialogEntityPayload(speakingEntity.getId()));
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            if (invSlot < this.inventory.size()) {
                if (!this.insertItem(originalStack, this.inventory.size(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(originalStack, 0, this.inventory.size(), false)) {
                return ItemStack.EMPTY;
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }

        return newStack;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return speakingEntity.isAlive() && speakingEntity.distanceTo(player) < 10;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        boolean isClient = player.getWorld().isClient;
        if (!isClient) {
            boolean cannotGiveToPlayer = !player.isAlive() || player instanceof ServerPlayerEntity serverPlayer && serverPlayer.isDisconnected();
            for (var i = 0; i < inventory.size(); i++) {
                ItemStack itemStack = inventory.removeStack(i);
                if (cannotGiveToPlayer) {
                    if (!itemStack.isEmpty()) {
                        player.dropItem(itemStack, false);
                    }
                } else if (player instanceof ServerPlayerEntity) {
                    player.getInventory().offerOrDrop(itemStack);
                }
            }
            DialogHelper.screenInventories.remove(player.getUuid());
        }
    }
}
