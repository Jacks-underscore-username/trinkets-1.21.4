package jacksunderscoreusername.trinkets.trinkets.fire_wand;

import jacksunderscoreusername.trinkets.Main;
import jacksunderscoreusername.trinkets.Utils;
import jacksunderscoreusername.trinkets.payloads.SwingHandPayload;
import jacksunderscoreusername.trinkets.trinkets.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.item.FireChargeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static jacksunderscoreusername.trinkets.trinkets.TrinketDataComponent.TRINKET_DATA;

public class FireWand extends Trinket implements TrinketWithModes {
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
                .component(TRINKET_DATA, new TrinketDataComponent.TrinketData(1, " ", 0, 0))
                .rarity(Rarity.RARE)
                .component(AbstractModeDataComponent.ABSTRACT_MODE, new AbstractModeDataComponent.AbstractMode(0));
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

    @Override
    public int getMaxModes() {
        return 3;
    }

    @Override
    public String getModeName(int mode) {
        return switch (mode) {
            case 0 -> "Ranged Explosion";
            case 1 -> "Ranged Fiery Explosion";
            case 2 -> "Meteor Shower";
            default -> "";
        };
    }

    public static int getRange(int level) {
        return 50 + (level - 1) * 25;
    }

    public static int getMaxExplosionPower(int level) {
        return 3 + (level - 1) * 2;
    }

    public static int getMeteorExplosionPower(int level) {
        return level;
    }

    public static int getCooldown(int level) {
        return Integer.max(60, 300 - (level - 1) * 60);
    }

    public void initialize() {
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient) {
            return ActionResult.PASS;
        }
        ItemStack itemStack = hand.equals(Hand.MAIN_HAND) ? user.getMainHandStack() : user.getOffHandStack();
        if (!itemStack.isOf(Trinkets.FIRE_WAND)) {
            return ActionResult.PASS;
        }
        if (user.isSneaking()) {
            nextMode(itemStack, (ServerPlayerEntity) user);
            return ActionResult.SUCCESS;
        }
        if (!Trinkets.canPlayerUseTrinkets(user)) {
            return ActionResult.PASS;
        }
        if (itemStack.get(CooldownDataComponent.COOLDOWN) != null) {
            return ActionResult.PASS;
        }
        int level = Objects.requireNonNull(itemStack.get(TRINKET_DATA)).level();

        int mode = Objects.requireNonNull(itemStack.get(AbstractModeDataComponent.ABSTRACT_MODE)).mode();

        if (mode == 0 || mode == 1) {
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
            for (int i = 0; i < range; i++) {
                Vec3d pos = startPos.lerp(endPos, i / ((double) range));
                BlockState blockState = world.getBlockState(new BlockPos((int) Math.round(pos.x), (int) Math.round(pos.y), (int) Math.round(pos.z)));
                if (!blockState.isAir() && blockState.getFluidState().isEmpty() || i == range - 1) {
                    explosions.add(new DelayedExplosion(world, pos.x, pos.y, pos.z, user, i / 5, getMaxExplosionPower(level), mode == 1));
                    break;
                } else
                    explosions.add(new DelayedExplosion(world, pos.x, pos.y, pos.z, user, i / 5, 1, mode == 1));
            }

            ((ServerWorld) world).addEntities(explosions.stream());
        } else if (mode == 2) {
            int range = getRange(level);
            int explosionCount = (int) (Math.PI * range * range / 250);
            Random random = world.random;
            for (int i = 0; i < explosionCount; i++) {
                Vec3d pos = new Vec3d(user.getX() + (random.nextFloat() * 2 - 1) * range, user.getY(), user.getZ() + (random.nextFloat() * 2 - 1) * range);
                if (user.getBlockPos().getManhattanDistance(BlockPos.ofFloored(pos)) > range) {
                    i--;
                    continue;
                }
                FireballEntity fireballEntity = new FireballEntity(world, user, new Vec3d(0, -random.nextFloat(), 0), getMeteorExplosionPower(level));
                fireballEntity.setPosition(pos.x, world.getTopYInclusive() + random.nextFloat() * 200, pos.z);
                world.spawnEntity(fireballEntity);
            }
        }
        itemStack.set(CooldownDataComponent.COOLDOWN, new CooldownDataComponent.CooldownData(Objects.requireNonNull(world.getServer()).getTicks(), getCooldown(level), getCooldown(level)));
        markUsed(itemStack, user);
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
        int range = getRange(level);
        int maxPower = getMaxExplosionPower(level);
        int meteorPower = getMeteorExplosionPower(level);

        int mode = Objects.requireNonNull(stack.get(AbstractModeDataComponent.ABSTRACT_MODE)).mode();

        Formatting color = Trinkets.getTrinketColor(this);

        tooltip.add(Text.literal("Mode: ").append(Text.literal(getModeName(mode)).formatted(color, Formatting.ITALIC)));

        if (mode == 0 || mode == 1) {
            tooltip.add(Text.literal("Right click with this item to").formatted(color));
            tooltip.add(Text.literal("shoot an explosive ").formatted(color).append(Text.literal(String.valueOf(range)).formatted(color, Formatting.BOLD).append(Text.literal(" blocks").formatted(color))));
            tooltip.add(Text.literal("away with a power of ").formatted(color).append(Text.literal(String.valueOf(maxPower)).formatted(color, Formatting.BOLD)));
            tooltip.add(Text.literal("that does" + (mode == 0 ? " not" : "") + " create fire").formatted(color));
        } else if (mode == 2) {
            tooltip.add(Text.literal("Right click with this item to").formatted(color));
            tooltip.add(Text.literal("call down meteors in a ").formatted(color).append(Text.literal(String.valueOf(range)).formatted(color, Formatting.BOLD).append(Text.literal(" block range").formatted(color))));
            tooltip.add(Text.literal("with a power of ").formatted(color).append(Text.literal(String.valueOf(meteorPower)).formatted(color, Formatting.BOLD)));
        }
    }
}