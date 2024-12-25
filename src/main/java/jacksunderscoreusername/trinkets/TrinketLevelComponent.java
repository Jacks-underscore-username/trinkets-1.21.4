package jacksunderscoreusername.trinkets;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;

// A component is needed to store data in an item, so this is the component used for storing the level of a trinket.
public class TrinketLevelComponent {
    public record TrinketLevel(int level) {
        public static final Codec<TrinketLevel> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                Codecs.POSITIVE_INT.optionalFieldOf("level", 1).forGetter(TrinketLevel::level)
        ).apply(builder, TrinketLevel::new));
    }

    public static final ComponentType<TrinketLevel> TRINKET_LEVEL = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(Main.MOD_ID, "trinket_level"),
            ComponentType.<TrinketLevel>builder().codec(TrinketLevel.CODEC).build()
    );
}
