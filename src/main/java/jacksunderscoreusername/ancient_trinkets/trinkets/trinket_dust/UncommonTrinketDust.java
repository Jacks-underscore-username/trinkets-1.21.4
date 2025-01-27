package jacksunderscoreusername.ancient_trinkets.trinkets.trinket_dust;

import jacksunderscoreusername.ancient_trinkets.trinkets.Trinket;
import jacksunderscoreusername.ancient_trinkets.trinkets.TrinketDataComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Rarity;
import net.minecraft.world.World;

import java.util.List;

public class UncommonTrinketDust extends Item {
    public static String id = "uncommon_trinket_dust";

    public String getId() {
        return id;
    }

    public static Settings getSettings() {
        return new Settings().rarity(Rarity.UNCOMMON);
    }

    public UncommonTrinketDust(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack item = user.getOffHandStack();
        if (hand.equals(Hand.MAIN_HAND) && item.getItem() instanceof Trinket && item.getRarity().equals(Rarity.UNCOMMON)) {
            if (!world.isClient) {
                TrinketDataComponent.TrinketData oldData = user.getOffHandStack().get(TrinketDataComponent.TRINKET_DATA);
                user.getOffHandStack().set(TrinketDataComponent.TRINKET_DATA, new TrinketDataComponent.TrinketData(oldData.level() + 1, oldData.UUID(), oldData.interference(), oldData.trackerCount()));
            }
            user.getMainHandStack().decrement(1);
            world.playSound(user, user.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1F, 1F);
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal("Right click while holding").formatted(Formatting.YELLOW, Formatting.ITALIC));
        tooltip.add(Text.literal("an uncommon or below trinket").formatted(Formatting.YELLOW, Formatting.ITALIC));
        tooltip.add(Text.literal("in your offhand to upgrade it").formatted(Formatting.YELLOW, Formatting.ITALIC));
    }
}
