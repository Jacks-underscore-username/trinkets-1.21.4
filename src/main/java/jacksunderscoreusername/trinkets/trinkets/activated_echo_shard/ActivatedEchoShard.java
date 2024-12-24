package jacksunderscoreusername.trinkets.trinkets.activated_echo_shard;

import jacksunderscoreusername.trinkets.Main;
import jacksunderscoreusername.trinkets.Trinket;
import jacksunderscoreusername.trinkets.TrinketCreationHandlers;
import jacksunderscoreusername.trinkets.Utils;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

import static jacksunderscoreusername.trinkets.trinkets.activated_echo_shard.StoredPortalComponent.STORED_PORTAL;

public class ActivatedEchoShard extends Trinket {

    public static String id = "activated_echo_shard";

    public String getId() {
        return id;
    }

    public static Settings getSettings() {
        return new Settings().rarity(Rarity.EPIC).maxCount(1).component(STORED_PORTAL, new StoredPortalComponent.StoredPortal(new BlockPos(0, 0, 0), World.OVERWORLD, false));
    }

    public ActivatedEchoShard(Settings settings) {
        super(settings);
    }

    public void initialize() {
        // Register this trinket with the creation handlers so that it can spawn on warden kill
        TrinketCreationHandlers.OnMobKill(EntityType.WARDEN, 10, this);

        // Setup all the used on portal interactions
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            ItemStack itemStack = player.getStackInHand(hand);
            Block targetedBlock = world.getBlockState(hitResult.getBlockPos()).getBlock();
            BlockPos pos = hitResult.getBlockPos();

            // Since this event triggers for both hands we need to check and make sure that the current hand is the one holding the item
            if (itemStack.getItem() instanceof ActivatedEchoShard) {
                StoredPortalComponent.StoredPortal itemData = itemStack.get(STORED_PORTAL);
                if (!player.isSneaking() && targetedBlock.equals(Blocks.NETHER_PORTAL)) {
                    boolean samePortal = itemData != null && Utils.areBothPointsConnected(itemData.pos(), itemData.dim(), hitResult.getBlockPos(), world.getRegistryKey(), Blocks.NETHER_PORTAL);
                    if (itemData == null || !itemData.hasPortal() || (!itemData.pos().equals(hitResult.getBlockPos())) && samePortal) {
                        player.playSound(SoundEvents.ENTITY_WARDEN_TENDRIL_CLICKS, 1.0F, 1.0F);
                        itemStack.set(STORED_PORTAL, new StoredPortalComponent.StoredPortal(hitResult.getBlockPos(), world.getRegistryKey(), true));
                        return ActionResult.SUCCESS;
                    } else if (!samePortal) {
                        if (Main.server.getWorld(itemData.dim()).getBlockState(itemData.pos()).getBlock().equals(Blocks.NETHER_PORTAL)) {
                            player.playSound(SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 1.0F, 0.75F);
                            if (!world.isClient) {
                                Utils.BoundingBox bounds1 = Utils.getConnectedBlocksBoundingBox(itemData.dim(), itemData.pos(), Blocks.NETHER_PORTAL);
                                Utils.BoundingBox bounds2 = Utils.getConnectedBlocksBoundingBox(world.getRegistryKey(), hitResult.getBlockPos(), Blocks.NETHER_PORTAL);
                                int color = Utils.hslToRgb(Math.random() * 360, 1, Math.random() * .5 + .25);
                                EchoPortalBlockEntity.defaultColor = color;
                                Utils.fillArea(Main.server.getWorld(itemData.dim()), SetupBlocks.ECHO_PORTAL.getStateWithProperties(world.getBlockState(itemData.pos())), bounds1);
                                Utils.fillArea(world, SetupBlocks.ECHO_PORTAL.getStateWithProperties(world.getBlockState(hitResult.getBlockPos())), bounds2);
                                for (var x = bounds1.min.getX(); x <= bounds1.max.getX(); x++) {
                                    for (var y = bounds1.min.getY(); y <= bounds1.max.getY(); y++) {
                                        for (var z = bounds1.min.getZ(); z <= bounds1.max.getZ(); z++) {
                                            EchoPortalBlockEntity blockEntity = (EchoPortalBlockEntity) Main.server.getWorld(itemData.dim()).getBlockEntity(new BlockPos(x, y, z));
                                            assert blockEntity != null;
                                            blockEntity.colorInt = color;
                                            blockEntity.dimension = world.getRegistryKey().getValue();
                                            blockEntity.teleportPos = bounds2.max;
                                            blockEntity.markDirty();
                                        }
                                    }
                                }
                                for (var x = bounds2.min.getX(); x <= bounds2.max.getX(); x++) {
                                    for (var y = bounds2.min.getY(); y <= bounds2.max.getY(); y++) {
                                        for (var z = bounds2.min.getZ(); z <= bounds2.max.getZ(); z++) {
                                            EchoPortalBlockEntity blockEntity = (EchoPortalBlockEntity) Main.server.getWorld(itemData.dim()).getBlockEntity(new BlockPos(x, y, z));
                                            assert blockEntity != null;
                                            blockEntity.colorInt = color;
                                            blockEntity.dimension = itemData.dim().getValue();
                                            blockEntity.teleportPos = bounds1.max;
                                            blockEntity.markDirty();

                                        }
                                    }
                                }
                            }
                        } else {
                            player.sendMessage(Text.literal("The other portal is broken").formatted(Formatting.RED), true);
                            player.playSound(SoundEvents.ENTITY_WARDEN_TENDRIL_CLICKS, 1.0F, 1.0F);
                        }
                        itemStack.set(STORED_PORTAL, new StoredPortalComponent.StoredPortal(new BlockPos(0, 0, 0), World.OVERWORLD, false));
                        return ActionResult.SUCCESS;
                    }
                } else if (!player.isSneaking() && targetedBlock.equals(SetupBlocks.ECHO_PORTAL)) {
                    EchoPortalBlockEntity blockEntity = (EchoPortalBlockEntity) world.getBlockEntity(pos);
                    assert blockEntity != null;
                    World otherWorld = Main.server.getWorld(RegistryKey.of(RegistryKeys.WORLD, blockEntity.dimension));
                    Utils.BoundingBox bounds1 = Utils.getConnectedBlocksBoundingBox(world.getRegistryKey(), pos, SetupBlocks.ECHO_PORTAL);
                    Utils.BoundingBox bounds2 = Utils.getConnectedBlocksBoundingBox(otherWorld.getRegistryKey(), blockEntity.teleportPos, SetupBlocks.ECHO_PORTAL);
                    int color = Utils.hslToRgb(Math.random() * 360, 1, Math.random() * .5 + .25);
                    for (var x = bounds1.min.getX(); x <= bounds1.max.getX(); x++) {
                        for (var y = bounds1.min.getY(); y <= bounds1.max.getY(); y++) {
                            for (var z = bounds1.min.getZ(); z <= bounds1.max.getZ(); z++) {
                                EchoPortalBlockEntity subBlockEntity = (EchoPortalBlockEntity) world.getBlockEntity(new BlockPos(x, y, z));
                                if (subBlockEntity == null) {
                                    continue;
                                }
                                subBlockEntity.colorInt = color;
                                subBlockEntity.markDirty();
                                world.updateNeighbors(new BlockPos(x, y, z), SetupBlocks.ECHO_PORTAL);
                            }
                        }
                    }
                    for (var x = bounds2.min.getX(); x <= bounds2.max.getX(); x++) {
                        for (var y = bounds2.min.getY(); y <= bounds2.max.getY(); y++) {
                            for (var z = bounds2.min.getZ(); z <= bounds2.max.getZ(); z++) {
                                EchoPortalBlockEntity subBlockEntity = (EchoPortalBlockEntity) otherWorld.getBlockEntity(new BlockPos(x, y, z));
                                if (subBlockEntity == null) {
                                    continue;
                                }
                                subBlockEntity.colorInt = color;
                                subBlockEntity.markDirty();
                                otherWorld.updateNeighbors(new BlockPos(x, y, z), SetupBlocks.ECHO_PORTAL);
                            }
                        }
                    }
                    return ActionResult.SUCCESS;
                }
            }
            return ActionResult.PASS;
        });

        SetupBlocks.initialize();
    }

    // Add shift right click detection to clear the stored portal (if any)
    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        if (itemStack.getItem() instanceof ActivatedEchoShard) {
            StoredPortalComponent.StoredPortal itemData = itemStack.get(STORED_PORTAL);
            if (user.isSneaking() && itemData != null && itemData.hasPortal()) {
                user.playSound(SoundEvents.ENTITY_WARDEN_TENDRIL_CLICKS, 1.0F, 1.0F);
                itemStack.set(STORED_PORTAL, new StoredPortalComponent.StoredPortal(new BlockPos(0, 0, 0), World.OVERWORLD, false));
                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.PASS;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        StoredPortalComponent.StoredPortal itemData = stack.get(STORED_PORTAL);
        if (itemData == null || !itemData.hasPortal()) {
            tooltip.add(Text.literal("No portal has been set").formatted(Formatting.ITALIC));
            tooltip.add(Text.literal("Right click a nether portal to link").formatted(Formatting.ITALIC));
        } else {
            tooltip.add(Text.literal("Linked to a portal at ").formatted(Formatting.DARK_PURPLE).append(Text.literal("(" + itemData.pos().getX() + ", " + itemData.pos().getY() + ", " + itemData.pos().getZ() + ") in dimension " + itemData.dim().getValue().getPath()).formatted(Formatting.AQUA)));
            tooltip.add(Text.literal("Right click a different nether portal to link them").formatted(Formatting.ITALIC));
            tooltip.add(Text.literal("SHIFT + Right click to clear linked portal").formatted(Formatting.ITALIC));
        }
    }
}
