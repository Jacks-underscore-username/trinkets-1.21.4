package jacksunderscoreusername.ancient_trinkets.trinkets;

import net.minecraft.item.ItemStack;

public interface TrinketWithCharges {
    int getMaxCharges(ItemStack stack);

    int getChargeTime(ItemStack stack);
}
