package jacksunderscoreusername.ancient_trinkets.trinkets;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import jacksunderscoreusername.ancient_trinkets.Main;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;

// A component is needed to store data in an item, so this is the component used for storing the level of a trinket.
public class TrinketDataComponent {
    public record TrinketData(int level, String UUID, int interference, int trackerCount) {
        public static final Codec<TrinketData> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                Codecs.POSITIVE_INT.fieldOf("level").forGetter(TrinketData::level),
                Codecs.NON_EMPTY_STRING.fieldOf("UUID").forGetter(TrinketData::UUID),
                Codecs.UNSIGNED_BYTE.fieldOf("interference").forGetter(TrinketData::interference),
                Codecs.NON_NEGATIVE_INT.fieldOf("trackerCount").forGetter(TrinketData::trackerCount)
        ).apply(builder, TrinketData::new));
    }

    public static final ComponentType<TrinketData> TRINKET_DATA = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(Main.MOD_ID, "trinket_data"),
            ComponentType.<TrinketData>builder().codec(TrinketData.CODEC).build()
    );
}
