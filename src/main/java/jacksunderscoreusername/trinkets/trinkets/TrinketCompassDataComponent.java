package jacksunderscoreusername.trinkets.trinkets;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import jacksunderscoreusername.trinkets.Main;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.GlobalPos;

public class TrinketCompassDataComponent {
    public record TrinketCompassData(String trinketUuid, String displayName) {
        public static final Codec<TrinketCompassData> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                Codecs.NON_EMPTY_STRING.fieldOf("UUID").forGetter(TrinketCompassData::trinketUuid),
                Codecs.NON_EMPTY_STRING.fieldOf("displayName").forGetter(TrinketCompassData::displayName)
        ).apply(builder, TrinketCompassData::new));
    }

    public static final ComponentType<TrinketCompassData> TRINKET_COMPASS = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(Main.MOD_ID, "trinket_compass"),
            ComponentType.<TrinketCompassData>builder().codec(TrinketCompassData.CODEC).build()
    );

    public static void initialize() {
    }
}
