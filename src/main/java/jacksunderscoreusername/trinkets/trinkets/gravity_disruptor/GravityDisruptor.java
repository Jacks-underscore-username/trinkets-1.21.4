package jacksunderscoreusername.trinkets.trinkets.gravity_disruptor;

import jacksunderscoreusername.trinkets.Main;
import jacksunderscoreusername.trinkets.Utils;
import jacksunderscoreusername.trinkets.payloads.SwingHandPayload;
import jacksunderscoreusername.trinkets.trinkets.CooldownDataComponent;
import jacksunderscoreusername.trinkets.trinkets.Trinket;
import jacksunderscoreusername.trinkets.trinkets.TrinketDataComponent;
import jacksunderscoreusername.trinkets.trinkets.Trinkets;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
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
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.List;
import java.util.Objects;

import static jacksunderscoreusername.trinkets.trinkets.TrinketDataComponent.TRINKET_DATA;

public class GravityDisruptor extends Trinket {
    public static String id = "gravity_disruptor";
    public static String name = "Gravity Disruptor";

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
                .component(TRINKET_DATA, new TrinketDataComponent.TrinketData(1, " ", 0))
                .rarity(Rarity.RARE);
        return settings;
    }

    public GravityDisruptor(Settings settings) {
        super(settings);
    }

    public static int getEffectRadius(int level) {
        return 10 + (level - 1) * 3;
    }

    public static int getMinEffectTime(int level) {
        return 10 + (level - 1);
    }

    public static int getMaxEffectTime(int level) {
        return 15 + (level - 1) * 2;
    }

    public static int getMaxEffectAmp(int level) {
        return level - 1;
    }

    public void initialize() {
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
        if (!itemStack.isOf(Trinkets.GRAVITY_DISRUPTOR)) {
            return ActionResult.PASS;
        }
        if (itemStack.get(CooldownDataComponent.COOLDOWN) != null) {
            return ActionResult.PASS;
        }
        int level = Objects.requireNonNull(itemStack.get(TRINKET_DATA)).level();
        int radius = getEffectRadius(level);
        int minTime = getMinEffectTime(level);
        int maxTime = getMaxEffectTime(level);
        int maxAmp = getMaxEffectAmp(level);
        List<Entity> entities = world.getOtherEntities(user, new Box(user.getX() - radius, user.getY() - radius, user.getZ() - radius, user.getX() + radius, user.getY() + radius, user.getZ() + radius));
        for (var entity : entities) {
            if (entity instanceof LivingEntity livingEntity) {
                StatusEffectInstance effect = new StatusEffectInstance(StatusEffects.LEVITATION, (int) Math.round((Math.random() * (maxTime - minTime) + minTime) * 20), (int) Math.round(Math.random() * maxAmp));
                livingEntity.addStatusEffect(effect, user);
            }
        }
        itemStack.set(CooldownDataComponent.COOLDOWN, new CooldownDataComponent.CooldownData(Objects.requireNonNull(world.getServer()).getTicks(), 5 * 60, 5 * 60));
        markUsed(itemStack, user);
        world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 1, 1);
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
        int radius = getEffectRadius(level);
        int minTime = getMinEffectTime(level);
        int maxTime = getMaxEffectTime(level);
        int maxAmp = getMaxEffectAmp(level);

        tooltip.add(Text.literal("Right click with this item to apply").formatted(Formatting.AQUA));
        tooltip.add(Text.literal("levitation 1" + (maxAmp == 0 ? "" : "-" + (maxAmp + 1)) + " to all other living entities").formatted(Formatting.AQUA));
        tooltip.add(Text.literal("within a " + radius * 2 + " block wide cube").formatted(Formatting.AQUA));
        tooltip.add(Text.literal("centered on you for " + minTime + "-" + maxTime + " seconds").formatted(Formatting.AQUA));
    }
}