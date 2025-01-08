package jacksunderscoreusername.trinkets.trinkets.soul_lamp;

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
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
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

public class SoulLamp extends Trinket {
    public static String id = "soul_lamp";
    public static String name = "Soul Lamp";

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
                .rarity(Rarity.EPIC);
        return settings;
    }

    public SoulLamp(Settings settings) {
        super(settings);
    }

    public static int getEffectRadius(int level) {
        return 20 + (level - 1) * 10;
    }

    public static int getEffectTime(int level) {
        return 90 * level;
    }

    public static int getEffectAmp(int level) {
        return level;
    }

    public static int getSpawnCount(int level) {
        return 10 + (level - 1) * 5;
    }

    public void initialize() {
        CursedEffect.initialize();
        Ghost.initialize();
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
        if (!itemStack.isOf(Trinkets.SOUL_LAMP)) {
            return ActionResult.PASS;
        }
        if (itemStack.get(CooldownDataComponent.COOLDOWN) != null) {
            return ActionResult.PASS;
        }
        int level = Objects.requireNonNull(itemStack.get(TRINKET_DATA)).level();
        int radius = getEffectRadius(level);
        int time = getEffectTime(level);
        int amp = getEffectAmp(level);
        List<Entity> entities = world.getOtherEntities(user, new Box(user.getX() - radius, user.getY() - radius, user.getZ() - radius, user.getX() + radius, user.getY() + radius, user.getZ() + radius));
        for (var entity : entities) {
            if (entity instanceof LivingEntity livingEntity) {
                StatusEffectInstance effect = new StatusEffectInstance(CursedEffect.CURSED, time * 20, amp);
                livingEntity.addStatusEffect(effect, user);
            }
        }
        for (var i = 0; i < getSpawnCount(level); i++)
            Ghost.GHOST.spawn((ServerWorld) world, user.getBlockPos(), SpawnReason.MOB_SUMMONED);
        itemStack.set(CooldownDataComponent.COOLDOWN, new CooldownDataComponent.CooldownData(Objects.requireNonNull(world.getServer()).getTicks(), 10 * 60, 10 * 60));
        markUsed(itemStack, user);
        world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.PLAYERS, 1.0F, 0.5F);
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
        int time = getEffectTime(level);
        int amp = getEffectAmp(level);
        int spawns = getSpawnCount(level);

        Formatting color = Trinkets.getTrinketColor(this);

        tooltip.add(Text.literal("Right click with this item to apply").formatted(color));
        tooltip.add(Text.literal("curse ").formatted(color).append(Text.literal(String.valueOf(amp + 1)).formatted(color, Formatting.BOLD)).append(Text.literal(" to all other living entities").formatted(color)));
        tooltip.add(Text.literal("within a ").formatted(color).append(Text.literal(String.valueOf(radius * 2)).formatted(color, Formatting.BOLD)).append(Text.literal(" block wide cube").formatted(color)));
        tooltip.add(Text.literal("centered on you for ").formatted(color).append(Text.literal(Utils.prettyTime(time, true)).formatted(color, Formatting.BOLD)));
        tooltip.add(Text.literal("and spawn ").formatted(color).append(Text.literal(String.valueOf(spawns)).formatted(color, Formatting.BOLD)).append(Text.literal(" ghosts").formatted(color)));
    }
}