package jacksunderscoreusername.trinkets.trinkets;

import net.minecraft.item.ItemStack;

abstract public class TrinketWithCharges extends Trinket {
    public TrinketWithCharges(Settings settings) {
        super(settings);
    }

    abstract public int getMaxCharges(ItemStack stack);

    abstract public int getChargeTime(ItemStack stack);
}
