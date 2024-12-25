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
        // Register this trinket with the creation handlers so that it can spawn on warden kill.
        TrinketCreationHandlers.OnMobKill(EntityType.WARDEN, 10, this);

        // Setup all the used on portal interactions.
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            ItemStack itemStack = player.getStackInHand(hand);
            BlockPos pos = hitResult.getBlockPos();
            Block targetedBlock = world.getBlockState(pos).getBlock();

            // Since this event triggers for both hands it needs to check and make sure that the current hand is the one holding the item.
            // It needs to make sure the player isn't sneaking since that is tied to other actions.
            if (itemStack.getItem() instanceof ActivatedEchoShard && !player.isSneaking() && targetedBlock.equals(Blocks.NETHER_PORTAL)) {

                // Stores the location / dimension of the saved portal (will be null if not set).
                StoredPortalComponent.StoredPortal itemData = itemStack.get(STORED_PORTAL);

                // See if the selected block is part of the same portal as is stored.
                boolean samePortal = itemData != null && Utils.areBothPointsConnected(itemData.pos(), itemData.dim(), pos, world.getRegistryKey(), Blocks.NETHER_PORTAL);

                // This branch handles setting the first portal, and so runs when either no portal is set or a portal is set the selected block is a part of that portal, but not the saved block.
                // This means you can change the saved block of the portal, and that the player never "uses" the item when that would do nothing.
                if (itemData == null || !itemData.hasPortal() || (!itemData.pos().equals(pos)) && samePortal) {

                    // Notify the player.
                    player.playSound(SoundEvents.ENTITY_WARDEN_TENDRIL_CLICKS, 1.0F, 1.0F);

                    // Save the data in the item of the selected block's location and dimension.
                    itemStack.set(STORED_PORTAL, new StoredPortalComponent.StoredPortal(pos, world.getRegistryKey(), true));

                    // Returns success so that the player swings and no further listeners fire.
                    return ActionResult.SUCCESS;
                } else if (!samePortal) {
                    // This branch handles trying to link two portals.

                    // TODO: make this work when the location is not loaded in the client.
                    // Check that a portal block still exists at the saved location / dimension in the item.
                    if (Main.server.getWorld(itemData.dim()).getBlockState(itemData.pos()).getBlock().equals(Blocks.NETHER_PORTAL)) {
                        player.playSound(SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 1.0F, 0.75F);

                        // This branch handles creating the portals and so only runs on the server.
                        if (!world.isClient) {

                            // The bounding box of the stored portal.
                            Utils.BoundingBox bounds1 = Utils.getConnectedBlocksBoundingBox(itemData.dim(), itemData.pos(), Blocks.NETHER_PORTAL);
                            // The bounding box of the currently selected portal.
                            Utils.BoundingBox bounds2 = Utils.getConnectedBlocksBoundingBox(world.getRegistryKey(), pos, Blocks.NETHER_PORTAL);

                            // Generate a random color for the portal pair.
                            int color = Utils.hslToRgb(Math.random() * 360, 1, Math.random() * .5 + .25);

                            // TODO: Make this better.
                            // Place the portal blocks for the stored portal location.
                            Utils.fillArea(Main.server.getWorld(itemData.dim()), SetupBlocks.ECHO_PORTAL.getStateWithProperties(world.getBlockState(itemData.pos())), bounds1);
                            // Place the portal blocks for the selected portal.
                            Utils.fillArea(world, SetupBlocks.ECHO_PORTAL.getStateWithProperties(world.getBlockState(pos)), bounds2);

                            // Set the data in each blockEntity in the stored portal's portal.
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

                            // Set the data in each blockEntity in the selected portal.
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
                        // This branch runs when there is no portal at the stored location / dimension.

                        // Notify the player.
                        player.sendMessage(Text.literal("The other portal is broken").formatted(Formatting.RED), true);
                        player.playSound(SoundEvents.ENTITY_WARDEN_TENDRIL_CLICKS, 1.0F, 1.0F);
                    }

                    // Remove the stored data from the item regardless of whether a portal was created or not.
                    itemStack.set(STORED_PORTAL, new StoredPortalComponent.StoredPortal(new BlockPos(0, 0, 0), World.OVERWORLD, false));

                    // Returns success so that the player swings and no further listeners fire.
                    return ActionResult.SUCCESS;
                }
            }

            // Returns pass so that the player does not swing their arm and the next event listener can run.
            return ActionResult.PASS;
        });

        // Links the echo portal block and block entity.
        SetupBlocks.initialize();
    }

    // Add shift right click detection to clear the stored portal (if any)
    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);

        // Since this event triggers for both hands it needs to check and make sure that the current hand is the one holding the item.
        if (itemStack.getItem() instanceof ActivatedEchoShard) {
            StoredPortalComponent.StoredPortal itemData = itemStack.get(STORED_PORTAL);

            // Check that the player is sneaking and that the item has a portal saved.
            if (user.isSneaking() && itemData != null && itemData.hasPortal()) {

                // Notify the user.
                user.playSound(SoundEvents.ENTITY_WARDEN_TENDRIL_CLICKS, 1.0F, 1.0F);

                // Remove the saved portal data from the item.
                itemStack.set(STORED_PORTAL, new StoredPortalComponent.StoredPortal(new BlockPos(0, 0, 0), World.OVERWORLD, false));

                // Returns success so that the player swings and no further listeners fire.
                return ActionResult.SUCCESS;
            }
        }

        // Returns pass so that the player does not swing their arm and the next event listener can run.
        return ActionResult.PASS;
    }

    // This runs effectively constantly whenever the tooltip is needed, so there is no need to trigger any event to update the tooltip.
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        StoredPortalComponent.StoredPortal itemData = stack.get(STORED_PORTAL);

        // If the item has no stored portal.
        if (itemData == null || !itemData.hasPortal()) {
            tooltip.add(Text.literal("No portal has been set").formatted(Formatting.ITALIC));
            tooltip.add(Text.literal("Right click a nether portal to link").formatted(Formatting.ITALIC));
        } else {
            // If the item has a stored portal (regardless of whether it still exists).
            tooltip.add(Text.literal("Linked to a portal at ").formatted(Formatting.DARK_PURPLE).append(Text.literal("(" + itemData.pos().getX() + ", " + itemData.pos().getY() + ", " + itemData.pos().getZ() + ") in dimension " + itemData.dim().getValue().getPath()).formatted(Formatting.AQUA)));
            tooltip.add(Text.literal("Right click a different nether portal to link them").formatted(Formatting.ITALIC));
            tooltip.add(Text.literal("SHIFT + Right click to clear linked portal").formatted(Formatting.ITALIC));
        }
    }
}
