package jacksunderscoreusername.trinkets.mixin;

import jacksunderscoreusername.trinkets.trinkets.Trinket;
import jacksunderscoreusername.trinkets.trinkets.TrinketDataComponent;
import jacksunderscoreusername.trinkets.trinkets.Trinkets;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GrindstoneScreenHandler;
import net.minecraft.util.Rarity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(targets = "net.minecraft.screen.GrindstoneScreenHandler$4")
public class GrindstoneOutputMixin {
    @Shadow
    @Final
    GrindstoneScreenHandler field_16780;

    @Inject(method = "onTakeItem", at = @At(value = "HEAD"))
    private void onTakeItem(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        ItemStack item = null;
        if (!this.field_16780.getSlot(0).getStack().isEmpty() && this.field_16780.getSlot(0).getStack().getItem() instanceof Trinket)
            item = this.field_16780.getSlot(0).getStack();
        else if (!this.field_16780.getSlot(1).getStack().isEmpty() && this.field_16780.getSlot(1).getStack().getItem() instanceof Trinket)
            item = this.field_16780.getSlot(1).getStack();
        if (item != null) {
            TrinketDataComponent.TrinketData data = item.get(TrinketDataComponent.TRINKET_DATA);
            assert data != null;
            Random random = new Random(data.UUID().hashCode());
            int count = Integer.min(stack.getItem().getMaxCount() - 1, 1 + random.nextInt(data.level()));
            stack.increment(count - 1);
            if (!player.getWorld().isClient)
                ((Trinket) item.getItem()).markRemoved(item);
        }
    }
}