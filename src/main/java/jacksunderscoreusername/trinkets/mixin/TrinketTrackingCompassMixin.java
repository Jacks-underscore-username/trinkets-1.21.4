package jacksunderscoreusername.trinkets.mixin;

import jacksunderscoreusername.trinkets.Main;
import jacksunderscoreusername.trinkets.trinkets.TrinketCompassDataComponent;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LodestoneTrackerComponent;
import net.minecraft.entity.Entity;
import net.minecraft.item.CompassItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.UUID;

@Mixin(CompassItem.class)
public class TrinketTrackingCompassMixin {
    @Inject(method = "inventoryTick", at = @At("HEAD"))
    private void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected, CallbackInfo ci) {
        if (world.isClient) return;
        TrinketCompassDataComponent.TrinketCompassData trackingData = stack.get(TrinketCompassDataComponent.TRINKET_COMPASS);
        if (trackingData == null)
            return;

        if (Main.state.data.lastTrinketLocations.get(UUID.fromString(trackingData.trinketUuid())) == null) {
            stack.remove(TrinketCompassDataComponent.TRINKET_COMPASS);
            stack.remove(DataComponentTypes.LODESTONE_TRACKER);
            return;
        }

        GlobalPos pos = Main.state.data.lastTrinketLocations.get(UUID.fromString(trackingData.trinketUuid())).pos;

        LodestoneTrackerComponent component = new LodestoneTrackerComponent(Optional.of(pos), false);
        LodestoneTrackerComponent oldData = stack.get(DataComponentTypes.LODESTONE_TRACKER);
        if (oldData == null || oldData.target().isEmpty() || !oldData.target().get().dimension().equals(pos.dimension()) || !oldData.target().get().pos().equals(pos.pos()))
            stack.set(DataComponentTypes.LODESTONE_TRACKER, component);
    }

    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void getName(ItemStack stack, CallbackInfoReturnable<Text> cir) {
        TrinketCompassDataComponent.TrinketCompassData trackingData = stack.get(TrinketCompassDataComponent.TRINKET_COMPASS);
        if (trackingData == null) return;
        cir.setReturnValue(Text.literal(trackingData.displayName()).formatted(Formatting.YELLOW));
    }
}
