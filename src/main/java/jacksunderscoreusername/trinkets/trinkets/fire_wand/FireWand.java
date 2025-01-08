package jacksunderscoreusername.trinkets.trinkets.fire_wand;

import jacksunderscoreusername.trinkets.Utils;
import jacksunderscoreusername.trinkets.trinkets.CooldownDataComponent;
import jacksunderscoreusername.trinkets.trinkets.Trinket;
import jacksunderscoreusername.trinkets.trinkets.TrinketDataComponent;
import jacksunderscoreusername.trinkets.trinkets.Trinkets;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static jacksunderscoreusername.trinkets.trinkets.TrinketDataComponent.TRINKET_DATA;

public class FireWand extends Trinket {
    public static String id = "fire_wand";
    public static String name = "Fire Wand";

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

    public FireWand(Settings settings) {
        super(settings);
    }

    public static final EntityType<DelayedExplosion> DELAYED_EXPLOSION = register(
            "delayed_explosion",
            EntityType.Builder.<DelayedExplosion>create(DelayedExplosion::new, SpawnGroup.MISC)
                    .dropsNothing()
                    .makeFireImmune()
                    .dimensions(0.98F, 0.98F)
                    .eyeHeight(0.15F)
                    .maxTrackingRange(10)
                    .trackingTickInterval(10)
    );

    private static <T extends Entity> EntityType<T> register(String id, EntityType.Builder<T> type) {
        return register(keyOf(id), type);
    }

    private static <T extends Entity> EntityType<T> register(RegistryKey<EntityType<?>> key, EntityType.Builder<T> type) {
        return Registry.register(Registries.ENTITY_TYPE, key, type.build(key));
    }

    private static RegistryKey<EntityType<?>> keyOf(String id) {
        return RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.ofVanilla(id));
    }

    public static int getRange(int level) {
        return 50 + (level - 1) * 25;
    }

    public static int getMaxExplosionPower(int level) {
        return 5 + (level - 1) * 3;
    }

    public void initialize() {
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!Trinkets.canPlayerUseTrinkets(user)) {
            return ActionResult.PASS;
        }
        ItemStack itemStack = hand.equals(Hand.MAIN_HAND) ? user.getMainHandStack() : user.getOffHandStack();
        if (!itemStack.isOf(Trinkets.FIRE_WAND)) {
            return ActionResult.PASS;
        }
        if (itemStack.get(CooldownDataComponent.COOLDOWN) != null) {
            return ActionResult.PASS;
        }
        int level = Objects.requireNonNull(itemStack.get(TRINKET_DATA)).level();
        int range = getRange(level);
        int startDistance = 5;
        float g = user.getYaw();
        float h = user.getPitch();
        float j = -MathHelper.sin(g * (float) (Math.PI / 180.0)) * MathHelper.cos(h * (float) (Math.PI / 180.0));
        float k = -MathHelper.sin(h * (float) (Math.PI / 180.0));
        float l = MathHelper.cos(g * (float) (Math.PI / 180.0)) * MathHelper.cos(h * (float) (Math.PI / 180.0));
        Vec3d direction = new Vec3d(j, k, l).normalize();
        Vec3d startPos = user.getPos().add(direction.multiply(startDistance));
        Vec3d endPos = startPos.add(direction.multiply(range + startDistance));
        List<Entity> explosions = new ArrayList<>();
        if (!world.isClient) {
            for (int i = 0; i < range; i++) {
                Vec3d pos = startPos.lerp(endPos, i / ((double) range));
                BlockState blockState = world.getBlockState(new BlockPos((int) Math.round(pos.x), (int) Math.round(pos.y), (int) Math.round(pos.z)));
                if (!blockState.isAir() && blockState.getFluidState().isEmpty() || i == range - 1) {
                    explosions.add(new DelayedExplosion(world, pos.x, pos.y, pos.z, user, i / 5, getMaxExplosionPower(level)));
                    break;
                } else
                    explosions.add(new DelayedExplosion(world, pos.x, pos.y, pos.z, user, i / 5, 1));
            }
        }
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.addEntities(explosions.stream());
            itemStack.set(CooldownDataComponent.COOLDOWN, new CooldownDataComponent.CooldownData(Objects.requireNonNull(world.getServer()).getTicks(), 60, 60));
            markUsed(itemStack, user);
        }
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
        int range = getRange(level);
        int maxPower = getMaxExplosionPower(level);

        Formatting color = Trinkets.getTrinketColor(this);

        tooltip.add(Text.literal("Right click with this item to apply").formatted(color));
        tooltip.add(Text.literal("shoot an explosive ").formatted(color).append(Text.literal(String.valueOf(range)).formatted(color, Formatting.BOLD).append(Text.literal(" blocks").formatted(color))));
        tooltip.add(Text.literal("with a power of ").formatted(color).append(Text.literal(String.valueOf(maxPower)).formatted(color, Formatting.BOLD)));
    }
}