package jacksunderscoreusername.trinkets.trinkets.activated_echo_shard;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import jacksunderscoreusername.trinkets.Main;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

// A component is needed to store data in an item, so this is the component used for the activated echo shard item, it stores the position of one block of the portal and the dimension of the portal, as well as whether or not it has a portal saved.
public class StoredPortalComponent {
    public record StoredPortal(BlockPos pos, RegistryKey<World> dim, boolean hasPortal) {
        public static final Codec<StoredPortal> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                BlockPos.CODEC.optionalFieldOf("pos", new BlockPos(0, 0, 0)).forGetter(StoredPortal::pos),
                World.CODEC.optionalFieldOf("dim", World.OVERWORLD).forGetter(StoredPortal::dim),
                Codec.BOOL.optionalFieldOf("hasPortal", false).forGetter(StoredPortal::hasPortal)
        ).apply(builder, StoredPortal::new));
    }

    public static final ComponentType<StoredPortal> STORED_PORTAL = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(Main.MOD_ID, "stored_portal"),
            ComponentType.<StoredPortal>builder().codec(StoredPortal.CODEC).build()
    );
}
