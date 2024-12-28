package jacksunderscoreusername.trinkets.trinkets.eternal_bonemeal;

import jacksunderscoreusername.trinkets.*;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import net.minecraft.world.biome.Biome;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static jacksunderscoreusername.trinkets.TrinketDataComponent.TRINKET_DATA;

public class EternalBonemeal extends Trinket {
    public static String id = "eternal_bonemeal";
    public static String name = "Eternal Bonemeal";

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
                .rarity(Rarity.UNCOMMON);
        return settings;
    }

    public EternalBonemeal(Settings settings) {
        super(settings);
    }

    public static int getRadius(int level) {
        return 5 + (level - 1) * 3;
    }

    public void initialize() {
        TrinketCreationHandlers.onBlockBreak((state) -> state.getBlock() instanceof CropBlock crop && crop.getAge(state) == crop.getMaxAge(), 150, this);
        TrinketCreationHandlers.onBlockBreak((state) -> state.getBlock() instanceof CropBlock crop && crop.getAge(state) == crop.getMaxAge(), 500, this, SoundEvents.BLOCK_GROWING_PLANT_CROP, 1, 1);
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
        if (!itemStack.isOf(Trinkets.ETERNAL_BONEMEAL)) {
            return ActionResult.PASS;
        }
        if (itemStack.get(CooldownDataComponent.COOLDOWN) != null) {
            return ActionResult.PASS;
        }
        int level = Objects.requireNonNull(itemStack.get(TRINKET_DATA)).level();
        int radius = getRadius(level);

        for (BlockPos pos : BlockPos.iterate((int) (Math.round(user.getX() + .5) - radius), (int) (Math.round(user.getY() + .5) - radius), (int) (Math.round(user.getZ() + .5) - radius), (int) (Math.round(user.getX() - .5) + radius), (int) (Math.round(user.getY() - .5) + radius), (int) (Math.round(user.getZ() - .5) + radius))) {
            BlockState block = world.getBlockState(pos);
            for (var side : new Direction[]{Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.DOWN}) {
                if (block.getBlock() instanceof Fertilizable fertilizable && fertilizable.isFertilizable(world, pos, block)) {
                    BlockState blockState = world.getBlockState(pos);
                    if (blockState.getBlock() instanceof SaplingBlock blockClass) {
                        blockClass.grow((ServerWorld) world, world.random, pos, blockState);
                    } else if (blockState.getBlock() instanceof CropBlock blockClass) {
                        world.setBlockState(pos, blockClass.withAge(blockClass.getMaxAge()), Block.NOTIFY_LISTENERS);
                    } else {
                        fertilizable.grow((ServerWorld) world, world.random, pos, block);
                    }
                    world.syncWorldEvent(WorldEvents.BONE_MEAL_USED, pos, 15);

                } else {
                    if (world.getBlockState(pos).isOf(Blocks.WATER) && world.getFluidState(pos).getLevel() == 8) {
                        Random random = world.getRandom();

                        label80:
                        for (int i = 0; i < 128; i++) {
                            BlockPos pos2 = pos;
                            BlockState blockState = Blocks.SEAGRASS.getDefaultState();

                            for (int j = 0; j < i / 16; j++) {
                                pos2 = pos2.add(random.nextInt(3) - 1, (random.nextInt(3) - 1) * random.nextInt(3) / 2, random.nextInt(3) - 1);
                                if (world.getBlockState(pos2).isFullCube(world, pos2)) {
                                    continue label80;
                                }
                            }

                            RegistryEntry<Biome> registryEntry = world.getBiome(pos2);
                            if (registryEntry.isIn(BiomeTags.PRODUCES_CORALS_FROM_BONEMEAL)) {
                                if (i == 0 && side.getAxis().isHorizontal()) {
                                    blockState = Registries.BLOCK
                                            .getRandomEntry(BlockTags.WALL_CORALS, world.random)
                                            .map(blockEntry -> blockEntry.value().getDefaultState())
                                            .orElse(blockState);
                                    if (blockState.contains(DeadCoralWallFanBlock.FACING)) {
                                        blockState = blockState.with(DeadCoralWallFanBlock.FACING, side);
                                    }
                                } else if (random.nextInt(4) == 0) {
                                    blockState = Registries.BLOCK
                                            .getRandomEntry(BlockTags.UNDERWATER_BONEMEALS, world.random)
                                            .map(blockEntry -> blockEntry.value().getDefaultState())
                                            .orElse(blockState);
                                }
                            }

                            if (blockState.isIn(BlockTags.WALL_CORALS, state -> state.contains(DeadCoralWallFanBlock.FACING))) {
                                for (int k = 0; !blockState.canPlaceAt(world, pos2) && k < 4; k++) {
                                    blockState = blockState.with(DeadCoralWallFanBlock.FACING, Direction.Type.HORIZONTAL.random(random));
                                }
                            }

                            if (blockState.canPlaceAt(world, pos2)) {
                                BlockState blockState2 = world.getBlockState(pos2);
                                if (blockState2.isOf(Blocks.WATER) && world.getFluidState(pos2).getLevel() == 8) {
                                    world.setBlockState(pos2, blockState, Block.NOTIFY_ALL);
                                } else if (blockState2.isOf(Blocks.SEAGRASS) && ((Fertilizable) Blocks.SEAGRASS).isFertilizable(world, pos2, blockState2) && random.nextInt(10) == 0) {
                                    ((Fertilizable) Blocks.SEAGRASS).grow((ServerWorld) world, random, pos2, blockState2);
                                }
                            }
                        }
                    }
                }
            }
        }
        itemStack.set(CooldownDataComponent.COOLDOWN, new CooldownDataComponent.CooldownData(Objects.requireNonNull(world.getServer()).getTicks(), 5 * 60, 5 * 60));
        markUsed(itemStack, user);
        world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_GROWING_PLANT_CROP, SoundCategory.PLAYERS, 1.0F, 1.0F);
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
        int radius = getRadius(level);

        tooltip.add(Text.literal("Right click with this item to grow").formatted(Formatting.YELLOW));
        tooltip.add(Text.literal("everything within the " + radius * 2 + " block").formatted(Formatting.YELLOW));
        tooltip.add(Text.literal("wide cube centered on you").formatted(Formatting.YELLOW));

        tooltip.add(Text.literal("Harvest fully grown crops while holding this for a chance to upgrade").formatted(Formatting.ITALIC, Formatting.YELLOW));
    }
}