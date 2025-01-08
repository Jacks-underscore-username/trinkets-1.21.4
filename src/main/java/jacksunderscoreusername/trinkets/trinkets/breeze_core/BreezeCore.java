package jacksunderscoreusername.trinkets.trinkets.breeze_core;

import jacksunderscoreusername.trinkets.Main;
import jacksunderscoreusername.trinkets.Utils;
import jacksunderscoreusername.trinkets.minix_io.BreezeCoreAntiFall;
import jacksunderscoreusername.trinkets.payloads.SwingHandPayload;
import jacksunderscoreusername.trinkets.trinkets.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.List;
import java.util.Objects;

import static jacksunderscoreusername.trinkets.trinkets.TrinketDataComponent.TRINKET_DATA;

public class BreezeCore extends TrinketWithCharges {
    public static String id = "breeze_core";
    public static String name = "Breeze Core";

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
                .rarity(Rarity.RARE)
                .component(ChargesDataComponent.CHARGES, new ChargesDataComponent.Charges(getMaxCharges(1)));
        return settings;
    }

    public BreezeCore(Settings settings) {
        super(settings);
    }

    public static int getForce(int level) {
        return 3 + (level - 1) * 2;
    }

    public static int getMaxCharges(int level) {
        return 20 + (level - 1) * 10;
    }

    public static int getChargeTime(int level) {
        return Integer.max(3, 300 - (level - 1) * 90);
    }

    public int getMaxCharges(ItemStack stack) {
        return getMaxCharges(Objects.requireNonNull(stack.get(TRINKET_DATA)).level());
    }

    public int getChargeTime(ItemStack stack) {
        return getChargeTime(Objects.requireNonNull(stack.get(TRINKET_DATA)).level());
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
        if (!itemStack.isOf(Trinkets.BREEZE_CORE)) {
            return ActionResult.PASS;
        }
        if (itemStack.get(ChargesDataComponent.CHARGES) == null || Objects.requireNonNull(itemStack.get(ChargesDataComponent.CHARGES)).charges() == 0) {
            return ActionResult.PASS;
        }

        int level = Objects.requireNonNull(itemStack.get(TRINKET_DATA)).level();

        int f = getForce(level);
        float g = user.getYaw();
        float h = user.getPitch();
        float j = -MathHelper.sin(g * (float) (Math.PI / 180.0)) * MathHelper.cos(h * (float) (Math.PI / 180.0));
        float k = -MathHelper.sin(h * (float) (Math.PI / 180.0));
        float l = MathHelper.cos(g * (float) (Math.PI / 180.0)) * MathHelper.cos(h * (float) (Math.PI / 180.0));
        float m = MathHelper.sqrt(j * j + k * k + l * l);
        j *= f / m;
        k *= f / m;
        l *= f / m;
        user.setVelocity(j, k, l);
        user.useRiptide(20, 0, null);
        int charges = Objects.requireNonNull(itemStack.get(ChargesDataComponent.CHARGES)).charges() - 1;
        if (charges == 0)
            itemStack.remove(ChargesDataComponent.CHARGES);
        else
            itemStack.set(ChargesDataComponent.CHARGES, new ChargesDataComponent.Charges(charges));
        ((BreezeCoreAntiFall) user).Trinkets_1_21_4_v2$setBreezeCoreAntiFall(world.getServer());
        ServerPlayNetworking.send(Objects.requireNonNull(Main.server.getPlayerManager().getPlayer(user.getUuid())), new UseBreezeCorePayload(new Vector3f(j, k, l)));
        world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_BREEZE_WIND_BURST.value(), SoundCategory.PLAYERS, 1, 1);

        markUsed(itemStack, user);
        ServerPlayNetworking.send(Objects.requireNonNull(Main.server.getPlayerManager().getPlayer(user.getUuid())), new SwingHandPayload(hand.equals(Hand.MAIN_HAND)));
        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        if (!((Trinket) stack.getItem()).shouldShowTooltip(stack, context, tooltip, type)) {
            return;
        }
        int level = Objects.requireNonNull(stack.get(TRINKET_DATA)).level();
        int force = getForce(level);
        int maxCharges = getMaxCharges(level);
        int chargeTime = getChargeTime(level);
        String time = stack.get(CooldownDataComponent.COOLDOWN) == null ? "" : Utils.prettyTime(Objects.requireNonNull(stack.get(CooldownDataComponent.COOLDOWN)).timeLeft(), false);

        Formatting color = Trinkets.getTrinketColor(this);

        int charges = stack.get(ChargesDataComponent.CHARGES) == null ? 0 : Objects.requireNonNull(stack.get(ChargesDataComponent.CHARGES)).charges();
        tooltip.add(Text.literal("Current charges: " + charges + " / ").formatted(color, Formatting.ITALIC).append(Text.literal(String.valueOf(maxCharges)).formatted(color, Formatting.ITALIC, Formatting.BOLD)));
        tooltip.add(Text.literal("Next charge: " + time).formatted(color, Formatting.ITALIC));

        tooltip.add(Text.literal("Right click with this item to consume").formatted(color));
        tooltip.add(Text.literal("1 charge and launch yourself").formatted(color));
        tooltip.add(Text.literal("forward with a force of " + force).formatted(color));
        tooltip.add(Text.literal("Gains 1 charge every ").formatted(color).append(Text.literal(Utils.prettyTime(chargeTime, true)).formatted(color, Formatting.BOLD)));
    }
}