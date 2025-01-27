package jacksunderscoreusername.trinkets.trinkets;

import jacksunderscoreusername.trinkets.Utils;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Objects;

public interface TrinketWithModes {
    int getMaxModes();

    String getModeName(int mode);

    default void nextMode(ItemStack stack) {
        nextMode(stack, null);
    }

    default void nextMode(ItemStack stack, ServerPlayerEntity player) {
        int mode = (getMode(stack) + 1) % getMaxModes();
        stack.set(AbstractModeDataComponent.ABSTRACT_MODE, new AbstractModeDataComponent.AbstractMode(mode));
        if (player != null) {
            player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1, 1);
            player.sendMessage(Text.literal(getModeName(mode)).formatted(Formatting.ITALIC, stack.getRarity().getFormatting()), true);
        }
        onModeChange(stack);
    }

    default void onModeChange(ItemStack stack) {
    }

    default int getMode(ItemStack stack) {
        if (!stack.contains(AbstractModeDataComponent.ABSTRACT_MODE))
            stack.set(AbstractModeDataComponent.ABSTRACT_MODE, new AbstractModeDataComponent.AbstractMode(0));
        return Objects.requireNonNull(stack.get(AbstractModeDataComponent.ABSTRACT_MODE)).mode();
    }
}
