package jacksunderscoreusername.trinkets;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

public class TrinketCreationHandlers {
    public static void OnBlockBreak(Block block, int chance, Trinket trinket) {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) -> {
            if (Trinkets.canTrinketBeCreated(trinket.getId()) && state.getBlock().equals(block) && Math.floor(Math.random() * chance) == 0) {
                Vec3d center = pos.toCenterPos();
                ItemEntity itemEntity = new ItemEntity(world, center.x, center.y, center.z, trinket.getDefaultStack());
                world.spawnEntity(itemEntity);
                trinket.markCreated(itemEntity.getStack());
            }
        });
    }

    public static void OnMobKill(EntityType type, int chance, Trinket trinket) {
        ServerLivingEntityEvents.AFTER_DEATH.register((killedEntity, damageSource) -> {
            if (Trinkets.canTrinketBeCreated(trinket.getId()) && damageSource.getAttacker() instanceof ServerPlayerEntity && killedEntity.getType().equals(type) && Math.floor(Math.random() * chance) == 0) {
                Vec3d pos = killedEntity.getPos();
                ItemEntity itemEntity = new ItemEntity(killedEntity.getWorld(), pos.x, pos.y, pos.z, trinket.getDefaultStack());
                killedEntity.getWorld().spawnEntity(itemEntity);
                trinket.markCreated(itemEntity.getStack());
            }
        });
    }
}
