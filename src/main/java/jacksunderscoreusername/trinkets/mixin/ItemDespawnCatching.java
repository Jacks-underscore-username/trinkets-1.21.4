package jacksunderscoreusername.trinkets.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import jacksunderscoreusername.trinkets.Main;
import jacksunderscoreusername.trinkets.Trinket;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemEntity.class)
public class ItemDespawnCatching {
    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/ItemEntity;discard()V",
                    ordinal = 1
            )
    )
    private void catchDespawn(ItemEntity instance, Operation<Void> original) {
        if (instance.getStack().getItem() instanceof Trinket trinket) trinket.markRemoved(instance.getStack());
        original.call(instance);
    }
}