package jacksunderscoreusername.trinkets.trinkets.suspicious_substance;

import jacksunderscoreusername.trinkets.*;
import jacksunderscoreusername.trinkets.payloads.SwingHandPayload;
import jacksunderscoreusername.trinkets.trinkets.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
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
import java.util.Objects;

import static jacksunderscoreusername.trinkets.trinkets.TrinketDataComponent.TRINKET_DATA;

public class SuspiciousSubstance extends Trinket {
    public static String id = "suspicious_substance";
    public static String name = "Suspicious Substance";

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return name;
    }

    public static Settings getSettings() {
        Settings settings = new Settings();
        if (Trinkets.getTrinketLimit(id) > 0) {
            settings = settings.maxDamage(Trinkets.getMaxDurability(id));
        }
        settings = settings
                .maxCount(1)
                .component(TRINKET_DATA, new TrinketDataComponent.TrinketData(1, "", 0))
                .rarity(Rarity.RARE);
        return settings;
    }

    public SuspiciousSubstance(Settings settings) {
        super(settings);
    }

    public static StatusEffectInstance[] getEffects(int level) {
        int duration = (10 + level * 10) * 20;
        if (level == 1) {
            return new StatusEffectInstance[]{new StatusEffectInstance(StatusEffects.SPEED, duration, 4, true, true)};
        }
        if (level == 2) {
            return new StatusEffectInstance[]{
                    new StatusEffectInstance(StatusEffects.SPEED, duration, 6, true, true),
                    new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, duration, 6, true, true),
                    new StatusEffectInstance(StatusEffects.JUMP_BOOST, duration, 2, true, true)};
        }
        if (level == 3) {
            return new StatusEffectInstance[]{
                    new StatusEffectInstance(StatusEffects.SPEED, duration, 9, true, true),
                    new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, duration, 9, true, true),
                    new StatusEffectInstance(StatusEffects.JUMP_BOOST, duration, 4, true, true),
                    new StatusEffectInstance(StatusEffects.NIGHT_VISION, duration, 0, true, true),
                    new StatusEffectInstance(StatusEffects.HASTE, duration, 1, true, true),
                    new StatusEffectInstance(StatusEffects.STRENGTH, duration, 1, true, true)};
        }
        if (level == 4) {
            return new StatusEffectInstance[]{
                    new StatusEffectInstance(StatusEffects.SPEED, duration, 14, true, true),
                    new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, duration, 14, true, true),
                    new StatusEffectInstance(StatusEffects.JUMP_BOOST, duration, 7, true, true),
                    new StatusEffectInstance(StatusEffects.NIGHT_VISION, duration, 0, true, true),
                    new StatusEffectInstance(StatusEffects.HASTE, duration, 2, true, true),
                    new StatusEffectInstance(StatusEffects.STRENGTH, duration, 2, true, true),
                    new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, duration, 0, true, true),
                    new StatusEffectInstance(StatusEffects.WATER_BREATHING, duration, 0, true, true)};
        }
        if (level == 5) {
            return new StatusEffectInstance[]{
                    new StatusEffectInstance(StatusEffects.SPEED, duration, 19, true, true),
                    new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, duration, 19, true, true),
                    new StatusEffectInstance(StatusEffects.JUMP_BOOST, duration, 9, true, true),
                    new StatusEffectInstance(StatusEffects.NIGHT_VISION, duration, 0, true, true),
                    new StatusEffectInstance(StatusEffects.HASTE, duration, 4, true, true),
                    new StatusEffectInstance(StatusEffects.STRENGTH, duration, 4, true, true),
                    new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, duration, 0, true, true),
                    new StatusEffectInstance(StatusEffects.WATER_BREATHING, duration, 0, true, true),
                    new StatusEffectInstance(StatusEffects.REGENERATION, duration, 2, true, true),
                    new StatusEffectInstance(StatusEffects.RESISTANCE, duration, 1, true, true)};
        }
        return new StatusEffectInstance[]{
                new StatusEffectInstance(StatusEffects.SPEED, duration, level * 5 - 1, true, true),
                new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, duration, level * 5 - 1, true, true),
                new StatusEffectInstance(StatusEffects.JUMP_BOOST, duration, level * 2, true, true),
                new StatusEffectInstance(StatusEffects.NIGHT_VISION, duration, 0, true, true),
                new StatusEffectInstance(StatusEffects.HASTE, duration, level - 1, true, true),
                new StatusEffectInstance(StatusEffects.STRENGTH, duration, level - 1, true, true),
                new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, duration, 0, true, true),
                new StatusEffectInstance(StatusEffects.WATER_BREATHING, duration, 0, true, true),
                new StatusEffectInstance(StatusEffects.REGENERATION, duration, level / 2, true, true),
                new StatusEffectInstance(StatusEffects.RESISTANCE, duration, 1 + level / 10, true, true)};
    }

    public void initialize() {
        TrinketCreationHandlers.onMobKill(EntityType.WITCH, 50, this);
        TrinketCreationHandlers.onMobKill(EntityType.WITCH, 25, this, SoundEvents.ENTITY_WITCH_DRINK, 1.0F, 1.0F);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient) {
            return ActionResult.PASS;
        }
        if (!Trinkets.canPlayerUseTrinkets(user)) {
            return ActionResult.PASS;
        }
        ItemStack itemStack = hand.equals(Hand.MAIN_HAND) ? user.getMainHandStack() : user.getOffHandStack();
        if (!itemStack.isOf(Trinkets.SUSPICIOUS_SUBSTANCE)) {
            return ActionResult.PASS;
        }
        if (itemStack.get(CooldownDataComponent.COOLDOWN) != null) {
            return ActionResult.PASS;
        }
        int level = Objects.requireNonNull(itemStack.get(TRINKET_DATA)).level();
        for (var effect : getEffects(level)) {
            user.addStatusEffect(effect);
        }
        itemStack.set(CooldownDataComponent.COOLDOWN, new CooldownDataComponent.CooldownData(Objects.requireNonNull(world.getServer()).getTicks(), 10 * 60, 10 * 60));
        markUsed(itemStack, user);
        world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_WITCH_DRINK, SoundCategory.PLAYERS, 1, 1);
        ServerPlayNetworking.send(Objects.requireNonNull(Main.server.getPlayerManager().getPlayer(user.getUuid())), new SwingHandPayload(hand.equals(Hand.MAIN_HAND)));
        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        if (!((Trinket) stack.getItem()).shouldShowTooltip(stack, context, tooltip, type)) {
            return;
        }

        if (stack.get(CooldownDataComponent.COOLDOWN) != null) {
            tooltip.add(Text.literal("Recharging for the next " + Utils.prettyTime(Objects.requireNonNull(stack.get(CooldownDataComponent.COOLDOWN)).timeLeft(), false)).formatted(Formatting.ITALIC, Formatting.BOLD));
        }

        int level = Objects.requireNonNull(stack.get(TRINKET_DATA)).level();
        StatusEffectInstance[] effects = getEffects(level);
        tooltip.add(Text.literal("Right click with this item to gain the following effects for " + Utils.prettyTime(effects[0].getDuration() / 20, true)).formatted(Formatting.AQUA));
        for (var effect : effects) {
            tooltip.add(Text.literal(" * " + effect.getEffectType().getIdAsString() + " " + (effect.getAmplifier() + 1)).formatted(Formatting.AQUA));
        }

        tooltip.add(Text.literal("Kill a witch while holding this for a chance to upgrade").formatted(Formatting.ITALIC, Formatting.AQUA));
    }
}