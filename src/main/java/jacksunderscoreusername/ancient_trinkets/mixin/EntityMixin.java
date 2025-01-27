package jacksunderscoreusername.ancient_trinkets.mixin;

import jacksunderscoreusername.ancient_trinkets.Main;
import jacksunderscoreusername.ancient_trinkets.trinkets.Trinket;
import jacksunderscoreusername.ancient_trinkets.trinkets.TrinketCompassDataComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.GlobalPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.UUID;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "onRemove", at = @At("HEAD"))
    private void onRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (!entity.getWorld().isClient && entity instanceof ItemEntity itemEntity) {
            if (itemEntity.getStack().getItem() instanceof Trinket trinket) trinket.markRemoved(itemEntity.getStack());
            if (itemEntity.getStack().isOf(Items.COMPASS)) {
                TrinketCompassDataComponent.TrinketCompassData data = itemEntity.getStack().get(TrinketCompassDataComponent.TRINKET_COMPASS);
                HashMap<UUID, Integer> compassCounts = Main.state.data.trinketCompasses;
                if (data != null && compassCounts.containsKey(UUID.fromString(data.trinketUuid()))) {
                    compassCounts.put(UUID.fromString(data.trinketUuid()), compassCounts.get(UUID.fromString(data.trinketUuid())) - itemEntity.getStack().getCount());
                }
            }
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tick(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (!entity.getWorld().isClient && entity instanceof ItemEntity itemEntity && itemEntity.getStack().getItem() instanceof Trinket trinket)
            trinket.updateLastPos(itemEntity.getStack(), new GlobalPos(entity.getWorld().getRegistryKey(), entity.getBlockPos()));
    }
}
