package jacksunderscoreusername.ancient_trinkets.trinkets;

import jacksunderscoreusername.ancient_trinkets.Main;
import jacksunderscoreusername.ancient_trinkets.StateSaverAndLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static jacksunderscoreusername.ancient_trinkets.trinkets.TrinketDataComponent.TRINKET_DATA;

abstract public class Trinket extends Item {
    public Trinket(Settings settings) {
        super(settings);
    }

    abstract public String getId();

    abstract public String getDisplayName();

    public void markCreated(ItemStack item) {
        TrinketDataComponent.TrinketData oldData = item.get(TrinketDataComponent.TRINKET_DATA);
        assert oldData != null;
        TrinketDataComponent.TrinketData newData = new TrinketDataComponent.TrinketData(oldData.level(), UUID.randomUUID().toString(), oldData.interference(), oldData.trackerCount());
        item.set(TrinketDataComponent.TRINKET_DATA, newData);
        Main.state.data.createdTrinkets.put(this.getId(), Main.state.data.createdTrinkets.getOrDefault(this.getId(), 0) + 1);
        if (Main.config.announce_spawns) {
            Main.server.getPlayerManager().broadcast(Text.literal("The trinket \"" + this.getDisplayName() + "\" has been found").formatted(Trinkets.rarityColors.get(this.getDefaultStack().getRarity())), false);
        }
        Main.LOGGER.info("Trinket \"{}\" was found", this.getId());
    }

    public void markRemoved(ItemStack item) {
        TrinketDataComponent.TrinketData data = Objects.requireNonNull(item.get(TRINKET_DATA));
        if (data.UUID().length() == 1) {
            return;
        }
        UUID trinketUuid = UUID.fromString(data.UUID());
        Main.state.data.createdTrinkets.put(this.getId(), Main.state.data.createdTrinkets.get(this.getId()) - 1);
        Main.state.data.currentTrinketPlayerMap.remove(trinketUuid);
        Main.state.data.claimedTrinketPlayerMap.remove(trinketUuid);
        Main.state.data.lastTrinketLocations.remove(trinketUuid);
        Main.state.data.trinketCompasses.remove(trinketUuid);
        if (Main.config.announce_destroys) {
            Main.server.getPlayerManager().broadcast(Text.literal("The trinket \"" + this.getDisplayName() + "\" has been lost").formatted(Trinkets.rarityColors.get(this.getDefaultStack().getRarity())), false);
        }
        Main.LOGGER.info("Trinket \"{}\" was lost", this.getId());
    }

    public void updateLastPos(ItemStack item, GlobalPos pos) {
        TrinketDataComponent.TrinketData data = Objects.requireNonNull(item.get(TRINKET_DATA));
        if (data.UUID().length() == 1)
            return;
        Main.state.data.lastTrinketLocations.put(UUID.fromString(data.UUID()), new StateSaverAndLoader.StoredData.lastTrinketLocationEntry(System.currentTimeMillis(), pos));
    }

    public void markUsed(ItemStack trinket, PlayerEntity user) {
        if (!(user instanceof ServerPlayerEntity)) {
            user = Main.server.getPlayerManager().getPlayer(user.getUuid());
            assert user != null;
        }
        Main.state.data.claimedTrinketPlayerMap.put(UUID.fromString(Objects.requireNonNull(trinket.get(TRINKET_DATA)).UUID()), user.getUuid());
        ArrayList<StateSaverAndLoader.StoredData.playerTrinketUseHistoryEntry> history = Main.state.data.playerTrinketUseHistory.computeIfAbsent(user.getUuid(), k -> new ArrayList<>());
        history.add(new StateSaverAndLoader.StoredData.playerTrinketUseHistoryEntry(Main.server.getTicks(), UUID.fromString(Objects.requireNonNull(trinket.get(TRINKET_DATA)).UUID())));
        Main.state.data.playerTrinketUseHistory.put(user.getUuid(), history);
        if (Trinkets.getMaxDurability(getId()) > 0) {
            if (!user.isCreative() && trinket.getDamage() + 1 >= trinket.getMaxDamage()) {
                ((Trinket) trinket.getItem()).markRemoved(trinket);
            }

            trinket.damage(1, user, ItemStack.areEqual(user.getMainHandStack(), trinket) ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient) {
            ((Trinket) stack.getItem()).updateLastPos(stack, new GlobalPos(world.getRegistryKey(), entity.getBlockPos()));
            if (entity instanceof PlayerEntity) {
                TrinketDataComponent.TrinketData data = stack.get(TRINKET_DATA);
                assert data != null;
                if (data.UUID().length() <= 1) {
                    markCreated(stack);
                    data = stack.get(TRINKET_DATA);
                    assert data != null;
                }
                UUID trinketUuid = UUID.fromString(data.UUID());
                int trackerCount = Main.state.data.trinketCompasses.getOrDefault(trinketUuid, 0);
                if (data.trackerCount() != trackerCount) {
                    stack.set(TRINKET_DATA, new TrinketDataComponent.TrinketData(data.level(), data.UUID(), data.interference(), trackerCount));
                    data = stack.get(TRINKET_DATA);
                }
                if (Main.state.data.claimedTrinketPlayerMap.containsKey(trinketUuid) && Main.state.data.claimedTrinketPlayerMap.get(trinketUuid).equals(entity.getUuid())) {
                    return;
                }
                StateSaverAndLoader.StoredData.currentTrinketPlayerMapEntry entry = Main.state.data.currentTrinketPlayerMap.get(trinketUuid);
                if (entry == null || !entry.player.equals(entity.getUuid())) {
                    Main.state.data.currentTrinketPlayerMap.put(trinketUuid, new StateSaverAndLoader.StoredData.currentTrinketPlayerMapEntry(entity.getUuid(), Main.server.getTicks()));
                } else if ((Main.server.getTicks() - entry.startTime) / 20 >= Main.config.trinket_interference_warmup) {
                    Main.state.data.claimedTrinketPlayerMap.put(trinketUuid, entity.getUuid());
                }
            }
        }
    }

    public boolean shouldShowTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        TrinketDataComponent.TrinketData data = stack.get(TRINKET_DATA);
        assert data != null;
        if (data.UUID().length() == 1)
            return false;
        if (data.interference() == 1) {
            tooltip.add(Text.literal("There is too much interference").formatted(Formatting.RED, Formatting.BOLD));
            tooltip.add(Text.literal("from your other ancient_trinkets").formatted(Formatting.RED, Formatting.BOLD));
            return false;
        }
        tooltip.add(Text.literal("Level " + data.level()).formatted(Formatting.ITALIC));
        if (data.trackerCount() == 1)
            tooltip.add(Text.literal("1 compass is tracking this").formatted(Formatting.ITALIC, Formatting.BOLD));
        else if (data.trackerCount() > 0)
            tooltip.add(Text.literal(data.trackerCount() + " compasses are tracking this").formatted(Formatting.ITALIC, Formatting.BOLD));
        return true;
    }

    public void initialize() {
        Main.LOGGER.warn("Trinket type \"{}\" has no initializeCreationHandler", this.getId());
    }
}


