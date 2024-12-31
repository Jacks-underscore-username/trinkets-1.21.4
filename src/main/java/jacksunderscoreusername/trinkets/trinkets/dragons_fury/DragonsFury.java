package jacksunderscoreusername.trinkets.trinkets.dragons_fury;

import jacksunderscoreusername.trinkets.*;
import jacksunderscoreusername.trinkets.payloads.SwingHandPayload;
import jacksunderscoreusername.trinkets.trinkets.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;
import java.util.Objects;

import static jacksunderscoreusername.trinkets.trinkets.TrinketDataComponent.TRINKET_DATA;

public class DragonsFury extends Trinket {
    public static String id = "dragons_fury";
    public static String name = "Dragons Fury";

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

    public DragonsFury(Settings settings) {
        super(settings);
    }

    public static int getRadius(int level) {
        return 10 + (level - 1) * 5;
    }

    public static int getDuration(int level) {
        return 15 + (level - 1) * 10;
    }

    public static int getAmplifier(int level) {
        return 1 + (level - 1) / 2;
    }

    public void initialize() {
        TrinketCreationHandlers.onMobKill(EntityType.ENDER_DRAGON, 2, this);
        TrinketCreationHandlers.onMobKill(EntityType.ENDER_DRAGON, 1, this, SoundEvents.ITEM_BOTTLE_FILL_DRAGONBREATH, 1.0F, 0.75F);

        VariedDragonFireball.initialize();
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
        if (!itemStack.isOf(Trinkets.DRAGONS_FURY)) {
            return ActionResult.PASS;
        }
        if (itemStack.get(CooldownDataComponent.COOLDOWN) != null) {
            return ActionResult.PASS;
        }

        int level = Objects.requireNonNull(itemStack.get(TRINKET_DATA)).level();
        VariedDragonFireball fireball = VariedDragonFireball.VARIED_DRAGON_FIREBALL.spawn((ServerWorld) world, new BlockPos(user.getBlockPos()), SpawnReason.MOB_SUMMONED);
        assert fireball != null;
        fireball.radius = getRadius(level);
        fireball.duration = getDuration(level) * 20;
        fireball.amplifier = getAmplifier(level);
        fireball.setOwner(user);
        fireball.addVelocity(user.raycast(1, 0, true).getPos().subtract(user.getEyePos()));

        itemStack.set(CooldownDataComponent.COOLDOWN, new CooldownDataComponent.CooldownData(Objects.requireNonNull(world.getServer()).getTicks(), 60, 60));
        markUsed(itemStack, user);
        world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_ENDER_DRAGON_SHOOT, SoundCategory.PLAYERS, 1, 1);
        ServerPlayNetworking.send(Objects.requireNonNull(Main.server.getPlayerManager().getPlayer(user.getUuid())), new SwingHandPayload(hand.equals(Hand.MAIN_HAND)));
        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        if (!((Trinket) stack.getItem()).shouldShowTooltip(stack, context, tooltip, type)) {
            return;
        }

        int level = Objects.requireNonNull(stack.get(TRINKET_DATA)).level();
        int radius = getRadius(level);
        int duration = getDuration(level);
        int amplifier = getAmplifier(level);

        tooltip.add(Text.literal("Right click with this item to shoot").formatted(Formatting.AQUA));
        tooltip.add(Text.literal("a dragon fireball that will create an").formatted(Formatting.AQUA));
        tooltip.add(Text.literal("effect cloud of instant damage " + (amplifier + 1)).formatted(Formatting.AQUA));
        tooltip.add(Text.literal("with a radius of " + radius + " for " + duration + " seconds").formatted(Formatting.AQUA));

        tooltip.add(Text.literal("Kill the ender dragon while holding this to upgrade").formatted(Formatting.AQUA, Formatting.ITALIC));

        if (stack.get(CooldownDataComponent.COOLDOWN) != null) {
            tooltip.add(Text.literal("Recharging for the next " + Utils.prettyTime(Objects.requireNonNull(stack.get(CooldownDataComponent.COOLDOWN)).timeLeft(), false)).formatted(Formatting.ITALIC, Formatting.BOLD));
        }
    }
}