package jacksunderscoreusername.trinkets.trinkets;

import net.minecraft.item.ItemStack;

import java.util.Objects;

public interface TrinketWithModes {
     int getMaxModes();

    default void nextMode(ItemStack stack) {
        stack.set(AbstractModeDataComponent.ABSTRACT_MODE, new AbstractModeDataComponent.AbstractMode((Objects.requireNonNull(stack.get(AbstractModeDataComponent.ABSTRACT_MODE)).mode() + 1) % getMaxModes()));
        onModeChange(stack);
    }

    default void onModeChange(ItemStack stack){}

    default int getMode(ItemStack stack) {
        return Objects.requireNonNull(stack.get(AbstractModeDataComponent.ABSTRACT_MODE)).mode();
    }
}
