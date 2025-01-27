package jacksunderscoreusername.ancient_trinkets.mixin;

import jacksunderscoreusername.ancient_trinkets.trinkets.Trinket;
import jacksunderscoreusername.ancient_trinkets.trinkets.Trinkets;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GrindstoneScreenHandler;
import net.minecraft.util.Rarity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GrindstoneScreenHandler.class)
public abstract class GrindstoneInputMixin {
    @Inject(method = "getOutputStack", at = @At("HEAD"), cancellable = true)
    public void getOutputStack(ItemStack firstInput, ItemStack secondInput, CallbackInfoReturnable<ItemStack> cir) {
        if (firstInput.isEmpty() == secondInput.isEmpty()) return;
        ItemStack item = firstInput.isEmpty() ? secondInput : firstInput;
        if (!(item.getItem() instanceof Trinket)) return;
        if (item.getRarity().equals(Rarity.EPIC))
            cir.setReturnValue(Trinkets.EPIC_TRINKET_DUST.getDefaultStack());
        if (item.getRarity().equals(Rarity.RARE))
            cir.setReturnValue(Trinkets.RARE_TRINKET_DUST.getDefaultStack());
        if (item.getRarity().equals(Rarity.UNCOMMON))
            cir.setReturnValue(Trinkets.UNCOMMON_TRINKET_DUST.getDefaultStack());
    }
}