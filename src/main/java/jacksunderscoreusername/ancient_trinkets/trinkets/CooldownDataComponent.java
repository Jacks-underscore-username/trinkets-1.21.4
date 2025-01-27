package jacksunderscoreusername.ancient_trinkets.trinkets;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import jacksunderscoreusername.ancient_trinkets.Main;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;

public class CooldownDataComponent {
    public record CooldownData(int startTime, int totalTime, int timeLeft) {
        public static final Codec<CooldownData> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                Codecs.NON_NEGATIVE_INT.fieldOf("start_time").forGetter(CooldownData::startTime),
                Codecs.NON_NEGATIVE_INT.fieldOf("total_time").forGetter(CooldownData::totalTime),
                Codecs.NON_NEGATIVE_INT.fieldOf("time_left").forGetter(CooldownData::timeLeft)
        ).apply(builder, CooldownData::new));
    }

    public static final ComponentType<CooldownData> COOLDOWN = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(Main.MOD_ID, "cooldown"),
            ComponentType.<CooldownData>builder().codec(CooldownData.CODEC).build()
    );

    public static void initialize(){}
}
