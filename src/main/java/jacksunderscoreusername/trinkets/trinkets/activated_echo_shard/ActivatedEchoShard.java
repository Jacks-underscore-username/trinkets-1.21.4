package jacksunderscoreusername.trinkets.trinkets.activated_echo_shard;

import jacksunderscoreusername.trinkets.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.List;
import java.util.Objects;

import static jacksunderscoreusername.trinkets.TrinketDataComponent.TRINKET_DATA;
import static jacksunderscoreusername.trinkets.trinkets.activated_echo_shard.StoredPortalComponent.STORED_PORTAL;

public class ActivatedEchoShard extends Trinket {

    public static final String id = "activated_echo_shard";
    public static final String name = "Activated Echo Shard";

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
                .rarity(Rarity.EPIC)
                .component(STORED_PORTAL, new StoredPortalComponent.StoredPortal(new BlockPos(0, 0, 0), World.OVERWORLD, false));
        return settings;
    }

    public ActivatedEchoShard(Settings settings) {
        super(settings);
    }

    public void initialize() {
        // Register this trinket with the creation handlers so that it can spawn on warden kill.
        TrinketCreationHandlers.OnMobKill(EntityType.WARDEN, 10, this);

        // Setup all the used on portal interactions.
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient) {
                return ActionResult.PASS;
            }

            if (!Trinkets.canPlayerUseTrinkets(player)) {
                return ActionResult.PASS;
            }

            ItemStack itemStack = player.getStackInHand(hand);
            BlockPos pos = hitResult.getBlockPos();
            BlockState targetedBlockState = world.getBlockState(pos);
            Block targetedBlock = targetedBlockState.getBlock();

            // Since this event triggers for both hands it needs to check and make sure that the current hand is the one holding the item.
            // It needs to make sure the player isn't sneaking since that is tied to other actions.
            if (itemStack.getItem() instanceof ActivatedEchoShard && !player.isSneaking() && targetedBlock.equals(Blocks.NETHER_PORTAL)) {

                // Stores the location / dimension of the saved portal (will be null if not set).
                StoredPortalComponent.StoredPortal itemData = itemStack.get(STORED_PORTAL);

                // See if the selected block is part of the same portal as is stored.
                Direction.Axis axis = (targetedBlockState.getOrEmpty(Properties.HORIZONTAL_AXIS).isEmpty() ? targetedBlock.getDefaultState().get(Properties.HORIZONTAL_AXIS) : targetedBlockState.get(Properties.HORIZONTAL_AXIS)).equals(Direction.Axis.X) ? Direction.Axis.Z : Direction.Axis.X;
                boolean samePortal = itemData != null && Utils.areBothPointsConnected(itemData.pos(), itemData.dim(), pos, world.getRegistryKey(), Blocks.NETHER_PORTAL, axis);

                // This branch handles setting the first portal, and so runs when either no portal is set or a portal is set the selected block is a part of that portal, but not the saved block.
                // This means you can change the saved block of the portal, and that the player never "uses" the item when that would do nothing.
                if (itemData == null || !itemData.hasPortal() || (!itemData.pos().equals(pos)) && samePortal) {

                    // Notify the player.
                    world.playSound(null, pos, SoundEvents.ENTITY_WARDEN_TENDRIL_CLICKS, SoundCategory.PLAYERS, 1.0F, 1.0F);

                    // Save the data in the item of the selected block's location and dimension.
                    itemStack.set(STORED_PORTAL, new StoredPortalComponent.StoredPortal(pos, world.getRegistryKey(), true));

                    // Returns success so that the player swings and no further listeners fire.
                    ServerPlayNetworking.send(Objects.requireNonNull(Main.server.getPlayerManager().getPlayer(player.getUuid())), new SwingHandPayload(hand.equals(Hand.MAIN_HAND)));
                    return ActionResult.SUCCESS;
                } else if (!samePortal) {
                    // This branch handles trying to link two portals.

                    //First see if the portal is too far.
                    StoredPortalComponent.StoredPortal portalComponent = itemStack.get(STORED_PORTAL);

                    int maxDistance = itemStack.get(TRINKET_DATA) == null ? 0 : Objects.requireNonNull(itemStack.get(TRINKET_DATA)).level() * 250;

                    // Calculate the positions as if they were in the overworld.
                    BlockPos overworldHere = world.getRegistryKey().equals(World.NETHER) ? pos.multiply(8).withY(pos.getY() / 8) : pos;
                    assert portalComponent != null;
                    BlockPos overworldTarget = portalComponent.dim().equals(World.NETHER) ? portalComponent.pos().multiply(8).withY(portalComponent.pos().getY()) : portalComponent.pos();
                    int distance = overworldHere.getManhattanDistance(overworldTarget);

                    // If they are too far.
                    if (distance > maxDistance) {
                        // Notify the player that they are too far.
                        player.sendMessage(Text.literal("The linked portal is " + (distance - maxDistance) + " blocks too far").formatted(Formatting.RED), true);
                        world.playSound(null, pos, SoundEvents.ENTITY_WARDEN_TENDRIL_CLICKS, SoundCategory.PLAYERS, 1.0F, 1.0F);

                        // Returns success so that the player swings and no further listeners fire.
                        ServerPlayNetworking.send(Objects.requireNonNull(Main.server.getPlayerManager().getPlayer(player.getUuid())), new SwingHandPayload(hand.equals(Hand.MAIN_HAND)));
                        return ActionResult.SUCCESS;
                    }

                    // Check that a portal block still exists at the saved location / dimension in the item.
                    if (Objects.requireNonNull(Main.server.getWorld(itemData.dim())).getBlockState(itemData.pos()).getBlock().equals(Blocks.NETHER_PORTAL)) {
                        world.playSound(null, pos, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 1.0F, 0.75F);

                        // The bounding box of the stored portal.
                        Utils.BoundingBox bounds1 = Utils.getConnectedBlocksBoundingBox(itemData.dim(), itemData.pos(), Blocks.NETHER_PORTAL, Objects.requireNonNull(Main.server.getWorld(itemData.dim())).getBlockState(itemData.pos()).getOrEmpty(Properties.HORIZONTAL_AXIS).orElse(Direction.Axis.X), Direction.Axis.Y);
                        // The bounding box of the currently selected portal.
                        Utils.BoundingBox bounds2 = Utils.getConnectedBlocksBoundingBox(world.getRegistryKey(), pos, Blocks.NETHER_PORTAL, world.getBlockState(pos).getOrEmpty(Properties.HORIZONTAL_AXIS).orElse(Direction.Axis.X), Direction.Axis.Y);

                        // Generate a random color for the portal pair.
                        int color = Utils.hslToRgb(Math.random() * 360, 1, Math.random() * .5 + .25);

                        // Store the other world.
                        World otherWorld = Main.server.getWorld(itemData.dim());
                        assert otherWorld != null;

                        // Place the portal blocks for the stored portal location, then set the data in each blockEntity in the selected portal.
                        Utils.fillArea(otherWorld, Setup.ECHO_PORTAL.getStateWithProperties(otherWorld.getBlockState(itemData.pos())), bounds1, blockEntity -> {
                            EchoPortalBlockEntity portalEntity = (EchoPortalBlockEntity) blockEntity;
                            assert blockEntity != null;

                            portalEntity.colorInt = color;
                            portalEntity.dimension = world.getRegistryKey().getValue();
                            portalEntity.teleportPos = bounds2.max;
                            portalEntity.checkForNetherPortal = true;
                            portalEntity.markDirty();
                        });

                        // Place the portal blocks for the selected portal, then set the data in each blockEntity in the stored portal's portal.
                        Utils.fillArea(world, Setup.ECHO_PORTAL.getStateWithProperties(world.getBlockState(pos)), bounds2, blockEntity -> {
                            EchoPortalBlockEntity portalEntity = (EchoPortalBlockEntity) blockEntity;
                            assert blockEntity != null;

                            portalEntity.colorInt = color;
                            portalEntity.dimension = otherWorld.getRegistryKey().getValue();
                            portalEntity.teleportPos = bounds1.max;
                            portalEntity.checkForNetherPortal = true;
                            portalEntity.markDirty();
                        });
                    } else {
                        // This branch runs when there is no portal at the stored location / dimension.

                        // Notify the player.
                        player.sendMessage(Text.literal("The other portal is broken").formatted(Formatting.RED), true);
                        world.playSound(null, pos, SoundEvents.ENTITY_WARDEN_TENDRIL_CLICKS, SoundCategory.PLAYERS, 1.0F, 1.0F);
                    }

                    // Remove the stored data from the item regardless of whether a portal was created or not.
                    itemStack.set(STORED_PORTAL, new StoredPortalComponent.StoredPortal(new BlockPos(0, 0, 0), World.OVERWORLD, false));

                    // Returns success so that the player swings and no further listeners fire.
                    ((Trinket) itemStack.getItem()).markUsed(itemStack, player);
                    ServerPlayNetworking.send(Objects.requireNonNull(Main.server.getPlayerManager().getPlayer(player.getUuid())), new SwingHandPayload(hand.equals(Hand.MAIN_HAND)));
                    return ActionResult.SUCCESS;
                }
            }

            // Returns pass so that the player does not swing their arm and the next event listener can run.
            return ActionResult.PASS;
        });

        // Setup the trinket upgrading logic.
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity) -> {
            if (
                    entity instanceof PlayerEntity &&
                            Trinkets.canPlayerUseTrinkets((PlayerEntity) entity) &&
                            (((PlayerEntity) entity).getMainHandStack().getItem().equals(Trinkets.ACTIVATED_ECHO_SHARD) ||
                                    ((PlayerEntity) entity).getOffHandStack().getItem().equals(Trinkets.ACTIVATED_ECHO_SHARD)) &&
                            killedEntity instanceof WardenEntity
            ) {
                boolean isMainHand = ((PlayerEntity) entity).getMainHandStack().getItem().equals(Trinkets.ACTIVATED_ECHO_SHARD);
                ItemStack item = isMainHand ? ((PlayerEntity) entity).getMainHandStack() : ((PlayerEntity) entity).getOffHandStack();
                TrinketDataComponent.TrinketData oldData = item.get(TRINKET_DATA);
                assert oldData != null;
                item.set(TRINKET_DATA, new TrinketDataComponent.TrinketData(oldData.level() + 1, oldData.UUID(), oldData.interference()));
                world.playSound(null, entity.getBlockPos(), SoundEvents.ENTITY_WARDEN_TENDRIL_CLICKS, SoundCategory.PLAYERS, 1.0F, 1.0F);
            }
        });

        // Links the echo portal block and block entity.
        Setup.initialize();
    }

    // Add shift right click detection to clear the stored portal (if any)
    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient) {
            return ActionResult.PASS;
        }

        if (!Trinkets.canPlayerUseTrinkets(user)) {
            return ActionResult.PASS;
        }

        ItemStack itemStack = user.getStackInHand(hand);

        // Since this event triggers for both hands it needs to check and make sure that the current hand is the one holding the item.
        if (itemStack.getItem() instanceof ActivatedEchoShard) {
            StoredPortalComponent.StoredPortal itemData = itemStack.get(STORED_PORTAL);

            // Check that the player is sneaking and that the item has a portal saved.
            if (user.isSneaking() && itemData != null && itemData.hasPortal()) {

                // Notify the user.
                world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_WARDEN_TENDRIL_CLICKS, SoundCategory.PLAYERS, 1.0F, 1.0F);

                // Remove the saved portal data from the item.
                itemStack.set(STORED_PORTAL, new StoredPortalComponent.StoredPortal(new BlockPos(0, 0, 0), World.OVERWORLD, false));

                // Returns success so that the player swings and no further listeners fire.
                ServerPlayNetworking.send(Objects.requireNonNull(Main.server.getPlayerManager().getPlayer(user.getUuid())), new SwingHandPayload(hand.equals(Hand.MAIN_HAND)));
                return ActionResult.SUCCESS;
            }
        }

        // Returns pass so that the player does not swing their arm and the next event listener can run.
        return ActionResult.PASS;
    }

    // This runs effectively constantly whenever the tooltip is needed, so there is no need to trigger any event to update the tooltip.
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        if (!((Trinket) stack.getItem()).shouldShowTooltip(stack, context, tooltip, type)) {
            return;
        }

        // Get the stored data in the item.
        StoredPortalComponent.StoredPortal itemData = stack.get(STORED_PORTAL);

        int maxDistance = stack.get(TRINKET_DATA) == null ? 0 : Objects.requireNonNull(stack.get(TRINKET_DATA)).level() * 250;

        // If the item has no stored portal.
        if (itemData == null || !itemData.hasPortal()) {
            tooltip.add(Text.literal("No portal has been set").formatted(Formatting.LIGHT_PURPLE));
            tooltip.add(Text.literal("Right click a nether portal to link").formatted(Formatting.LIGHT_PURPLE));
        } else {
            // If the item has a stored portal (regardless of whether it still exists).
            tooltip.add(Text.literal("Linked to a portal at ").formatted(Formatting.LIGHT_PURPLE).append(Text.literal("(" + itemData.pos().getX() + ", " + itemData.pos().getY() + ", " + itemData.pos().getZ() + ") in dimension " + itemData.dim().getValue().getPath()).formatted(Formatting.AQUA)));
            tooltip.add(Text.literal("Right click a different nether portal to link them").formatted(Formatting.LIGHT_PURPLE));
            tooltip.add(Text.literal("SHIFT + Right click to clear linked portal").formatted(Formatting.LIGHT_PURPLE));
        }
        tooltip.add(Text.literal("Max range: " + maxDistance + " blocks in the overworld").formatted(Formatting.LIGHT_PURPLE));
        tooltip.add(Text.literal("Kill a warden while holding this to upgrade").formatted(Formatting.LIGHT_PURPLE, Formatting.ITALIC));
    }
}