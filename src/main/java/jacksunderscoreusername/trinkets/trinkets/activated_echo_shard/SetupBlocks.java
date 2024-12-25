package jacksunderscoreusername.trinkets.trinkets.activated_echo_shard;

import jacksunderscoreusername.trinkets.Main;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

import java.util.function.Function;

// Handles registering the echoPortal block / block entity.
public class SetupBlocks {
    public static final EchoPortal ECHO_PORTAL = registerBlock("echo_portal", EchoPortal::new, Block.Settings.create().noCollision().strength(-1.0F).sounds(BlockSoundGroup.GLASS).luminance(state -> 11).pistonBehavior(PistonBehavior.BLOCK));

    private static <T extends Block> T registerBlock(String path, Function<Block.Settings, T> factory, Block.Settings settings) {
        final Identifier identifier = Identifier.of(Main.MOD_ID, path);
        final RegistryKey<Block> registryKey = RegistryKey.of(RegistryKeys.BLOCK, identifier);

        return (T) Blocks.register(registryKey, (Function<AbstractBlock.Settings, Block>) factory, settings);
    }

    public static final BlockEntityType<EchoPortalBlockEntity> ECHO_PORTAL_BLOCK_ENTITY = registerBlockEntity("echo_portal_block_entity", FabricBlockEntityTypeBuilder.create(EchoPortalBlockEntity::new, ECHO_PORTAL).build());

    public static <T extends BlockEntityType<?>> T registerBlockEntity(String path, T entity) {
        return Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(Main.MOD_ID, path), entity);
    }

    public static void initialize() {
    }
}