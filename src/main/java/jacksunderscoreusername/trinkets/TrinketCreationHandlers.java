package jacksunderscoreusername.trinkets;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.function.Predicate;

import static jacksunderscoreusername.trinkets.TrinketDataComponent.TRINKET_DATA;

public class TrinketCreationHandlers {
    private static void spawnTrinket(World world, Trinket trinket, Vec3d pos) {
        ItemEntity itemEntity = new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), trinket.getDefaultStack());
        world.spawnEntity(itemEntity);
        trinket.markCreated(itemEntity.getStack());
    }

    private static void upgradeTrinket(ItemStack item) {
        if (!(item.getItem() instanceof Trinket)) {
            throw new RuntimeException("Cannot upgrade a non trinket");
        }
        TrinketDataComponent.TrinketData oldData = item.get(TRINKET_DATA);
        item.set(TRINKET_DATA, new TrinketDataComponent.TrinketData(oldData.level() + 1, oldData.UUID(), oldData.interference()));
    }

    private static Hand getTrinketHand(PlayerEntity player, Trinket trinket) {
        if (player.getMainHandStack().isOf(trinket)) {
            return Hand.MAIN_HAND;
        }
        if (player.getOffHandStack().isOf(trinket)) {
            return Hand.OFF_HAND;
        }
        return null;
    }

    public static void onBlockBreak(Block block, int chance, Trinket trinket) {
        onBlockBreak(block, chance, trinket, null, 0, 0);
    }

    public static void onBlockBreak(Block block, int chance, Trinket trinket, SoundEvent upgradeSound, float volume, float pitch) {
        onBlockBreak((stack) -> stack.getBlock().equals(block), chance, trinket, upgradeSound, volume, pitch);
    }

    public static void onBlockBreak(Predicate<BlockState> func, int chance, Trinket trinket) {
        onBlockBreak(func, chance, trinket, null, 0, 0);
    }

    public static void onBlockBreak(Predicate<BlockState> func, int chance, Trinket trinket, SoundEvent upgradeSound, float volume, float pitch) {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) -> {
            if (func.test(state) && Math.floor(Math.random() * chance) == 0) {
                if (upgradeSound == null && Trinkets.canTrinketBeCreated(trinket.getId())) {
                    spawnTrinket(world, trinket, pos.toCenterPos());
                } else if (upgradeSound != null && getTrinketHand(player, trinket) != null) {
                    upgradeTrinket(player.getStackInHand(getTrinketHand(player, trinket)));
                    world.playSound(player, pos, upgradeSound, SoundCategory.PLAYERS, volume, pitch);
                }
            }
        });
    }

    public static void onMobKill(EntityType<?> type, int chance, Trinket trinket) {
        onMobKill(type, chance, trinket, null,0,0);
    }

    public static void onMobKill(EntityType<?> type, int chance, Trinket trinket, SoundEvent upgradeSound, float volume, float pitch) {
        onMobKill((entity) -> entity.getType().equals(type), chance, trinket, upgradeSound,volume,pitch);
    }

    public static void onMobKill(Predicate<LivingEntity> func, int chance, Trinket trinket) {
        onMobKill(func, chance, trinket, null,0,0);
    }

    public static void onMobKill(Predicate<LivingEntity> func, int chance, Trinket trinket, SoundEvent upgradeSound, float volume, float pitch) {
        ServerLivingEntityEvents.AFTER_DEATH.register((killedEntity, damageSource) -> {
            if (damageSource.getAttacker() instanceof ServerPlayerEntity player && func.test(killedEntity) && Math.floor(Math.random() * chance) == 0) {
                if (upgradeSound==null && Trinkets.canTrinketBeCreated(trinket.getId())) {
                    spawnTrinket(killedEntity.getWorld(), trinket, killedEntity.getPos());
                } else if (upgradeSound!=null && getTrinketHand(player, trinket) != null) {
                    upgradeTrinket(player.getStackInHand(getTrinketHand(player, trinket)));
                    player.getWorld().playSound(player, player.getBlockPos(), upgradeSound, SoundCategory.PLAYERS, volume, pitch);
                }
            }
        });
    }
}
