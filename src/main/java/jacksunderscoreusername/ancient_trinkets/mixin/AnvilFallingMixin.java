package jacksunderscoreusername.ancient_trinkets.mixin;

import jacksunderscoreusername.ancient_trinkets.trinkets.Trinkets;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

@Mixin(AnvilBlock.class)
public class AnvilFallingMixin {
    @Inject(method = "onLanding", at = @At("HEAD"))
    private void func(World world, BlockPos pos, BlockState fallingBlockState, BlockState currentStateInPos, FallingBlockEntity fallingBlockEntity, CallbackInfo ci) {
        Random random = world.random;
        ArrayList<ItemEntity> itemsToSpawn = new ArrayList<>();
        for (var entity : world.getEntitiesByClass(ItemEntity.class, new Box(pos), entity ->
                entity.getStack().isOf(Trinkets.UNCOMMON_TRINKET_DUST) ||
                        entity.getStack().isOf(Trinkets.RARE_TRINKET_DUST))) {
            int spawnCount = random.nextInt(entity.getStack().getCount());
            if (spawnCount > 0)
                itemsToSpawn.add(new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), (entity.getStack().isOf(Trinkets.UNCOMMON_TRINKET_DUST) ? Trinkets.RARE_TRINKET_DUST : Trinkets.EPIC_TRINKET_DUST).getDefaultStack().copyWithCount(spawnCount)));
            entity.remove(Entity.RemovalReason.DISCARDED);
        }
        for (var item : itemsToSpawn)
            world.spawnEntity(item);
    }
}
