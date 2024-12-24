package jacksunderscoreusername.trinkets.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import jacksunderscoreusername.trinkets.Trinket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemEntity.class)
public abstract class ItemDespawnCatching extends Entity {
    public ItemDespawnCatching(EntityType<?> type, World world) {
        super(type, world);
    }

    @WrapWithCondition(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/ItemEntity;discard()V",
                    ordinal = 1
            )
    )
    private boolean catchDespawn(ItemEntity instance) {
        Item item = instance.getStack().getItem();
        if (item instanceof Trinket) {
            ((Trinket) item).markRemoved();
        }
        return true;
    }
}