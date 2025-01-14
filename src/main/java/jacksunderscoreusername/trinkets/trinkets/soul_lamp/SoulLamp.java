package jacksunderscoreusername.trinkets.trinkets.soul_lamp;

import jacksunderscoreusername.trinkets.Main;
import jacksunderscoreusername.trinkets.StateSaverAndLoader;
import jacksunderscoreusername.trinkets.Utils;
import jacksunderscoreusername.trinkets.payloads.SwingHandPayload;
import jacksunderscoreusername.trinkets.trinkets.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
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

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static jacksunderscoreusername.trinkets.trinkets.TrinketDataComponent.TRINKET_DATA;

public class SoulLamp extends Trinket implements TrinketWithModes {
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
                .component(TRINKET_DATA, new TrinketDataComponent.TrinketData(1, " ", 0, 0))
                .rarity(Rarity.EPIC)
                .component(AbstractModeDataComponent.ABSTRACT_MODE, new AbstractModeDataComponent.AbstractMode(0));
        return settings;
    }

    public SoulLamp(Settings settings) {
        super(settings);
    }

    @Override
    public int getMaxModes() {
        return 4;
    }

    @Override
    public String getModeName(int mode) {
        return switch (mode) {
            case 0 -> "Manual Targeting";
            case 1 -> "Entity Type Targeting";
            case 2 -> "Current Area Targeting";
            case 3 -> "Continuous Area Targeting";
            default -> "";
        };
    }

    public static int getSpawnCount(int level) {
        return 3 + (level - 1) * 2;
    }

    public static int getLifeTime(int level) {
        return (int) (5 * 60 + (level - 1) * 2.5 * 60);
    }

    public static int getSoulMultiplier(int level) {
        return 2 + (level - 1);
    }

    public void initialize() {
        GhostEntity.initialize();
        ServerTickEvents.START_WORLD_TICK.register(world -> {
            for (var pair : Main.state.data.soulLampGroups.entrySet()) {
                StateSaverAndLoader.StoredData.soulLampEntry entry = pair.getValue();
                if (entry.lifeTimeLeft > 0)
                    entry.lifeTimeLeft--;
                if (entry.members.isEmpty())
                    Main.state.data.soulLampGroups.remove(pair.getKey());

            }
        });
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient) {
            return ActionResult.PASS;
        }
        ItemStack itemStack = hand.equals(Hand.MAIN_HAND) ? user.getMainHandStack() : user.getOffHandStack();
        if (!itemStack.isOf(Trinkets.SOUL_LAMP)) {
            return ActionResult.PASS;
        }
        if (user.isSneaking()) {
            nextMode(itemStack, (ServerPlayerEntity) user);
            return ActionResult.SUCCESS;
        }
        if (!Trinkets.canPlayerUseTrinkets(user)) {
            return ActionResult.PASS;
        }
        TrinketDataComponent.TrinketData data = itemStack.get(TRINKET_DATA);
        assert data != null;
        StateSaverAndLoader.StoredData.soulLampEntry oldGroup = Main.state.data.soulLampGroups.get(UUID.fromString(data.UUID()));
        if (oldGroup != null && getMode(itemStack) != oldGroup.mode) {
            oldGroup.mode = getMode(itemStack);
            if (getMode(itemStack) == 2)
                for (var newTarget : world.getEntitiesByClass(LivingEntity.class, new Box(user.getPos().subtract(GhostEntity.ENTITY_SEARCH_RADIUS), user.getPos().add(GhostEntity.ENTITY_SEARCH_RADIUS)), entity -> !oldGroup.playerUuid.equals(entity.getUuid()) && !oldGroup.members.contains(entity.getUuid()) && !oldGroup.targets.contains(entity.getUuid())))
                    oldGroup.targets.add(newTarget.getUuid());

            world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0F, 1.0F);
            ServerPlayNetworking.send(Objects.requireNonNull(Main.server.getPlayerManager().getPlayer(user.getUuid())), new SwingHandPayload(hand.equals(Hand.MAIN_HAND)));
            return ActionResult.SUCCESS;
        }
        if (itemStack.get(CooldownDataComponent.COOLDOWN) != null) {
            return ActionResult.PASS;
        }
        int level = data.level();
        int spawnCount = getSpawnCount(level);
        int lifeTime = getLifeTime(level);
        int soulMultiplier = getSoulMultiplier(level);
        StateSaverAndLoader.StoredData.soulLampEntry group = new StateSaverAndLoader.StoredData.soulLampEntry(user.getUuid(), lifeTime * 20L, soulMultiplier, new HashSet<>(), new HashSet<>(), new HashSet<>(), getMode(itemStack));
        Main.state.data.soulLampGroups.put(UUID.fromString(data.UUID()), group);
        for (var i = 0; i < spawnCount; i++) {
            GhostEntity ghost = GhostEntity.GHOST.spawn((ServerWorld) world, user.getBlockPos(), SpawnReason.MOB_SUMMONED);
            assert ghost != null;
            group.members.add(ghost.getUuid());
            ghost.group = group;
        }
        if (getMode(itemStack) == 2)
            for (var newTarget : world.getEntitiesByClass(LivingEntity.class, new Box(user.getPos().subtract(GhostEntity.ENTITY_SEARCH_RADIUS), user.getPos().add(GhostEntity.ENTITY_SEARCH_RADIUS)), entity -> !group.playerUuid.equals(entity.getUuid()) && !group.members.contains(entity.getUuid()) && !group.targets.contains(entity.getUuid())))
                group.targets.add(newTarget.getUuid());
        itemStack.set(CooldownDataComponent.COOLDOWN, new CooldownDataComponent.CooldownData(Objects.requireNonNull(world.getServer()).getTicks(), Integer.max(20 * 60, (int) (lifeTime * 1.25)), Integer.max(20 * 60, (int) (lifeTime * 1.25))));
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

        int level = Objects.requireNonNull(stack.get(TRINKET_DATA)).level();
        int spawnCount = getSpawnCount(level);
        int lifeTime = getLifeTime(level);
        int soulMultiplier = getSoulMultiplier(level);

        Formatting color = Trinkets.getTrinketColor(this);

        int mode = Objects.requireNonNull(stack.get(AbstractModeDataComponent.ABSTRACT_MODE)).mode();

        tooltip.add(Text.literal("Mode: ").append(Text.literal(getModeName(mode)).formatted(color, Formatting.ITALIC)));

        if (stack.get(CooldownDataComponent.COOLDOWN) != null) {
            tooltip.add(Text.literal("Recharging for the next " + Utils.prettyTime(Objects.requireNonNull(stack.get(CooldownDataComponent.COOLDOWN)).timeLeft(), false)).formatted(Formatting.ITALIC, Formatting.BOLD));
        }

        tooltip.add(Text.literal("Right click with this item to spawn").formatted(color));
        tooltip.add(Text.literal("").append(Text.literal(String.valueOf(spawnCount)).formatted(color, Formatting.BOLD)).append(Text.literal(" tamed level ").formatted(color)).append(Text.literal(String.valueOf(soulMultiplier)).formatted(color, Formatting.BOLD)).append(Text.literal(" ghosts with a max").formatted(color)));
        tooltip.add(Text.literal("lifetime of ").formatted(color).append(Text.literal(Utils.prettyTime(lifeTime, true)).formatted(color, Formatting.BOLD)));
    }
}