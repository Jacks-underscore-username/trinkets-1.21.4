package jacksunderscoreusername.ancient_trinkets.trinkets;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import jacksunderscoreusername.ancient_trinkets.Main;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;

public class AbstractModeDataComponent {
    public record AbstractMode(int mode) {
        public static final Codec<AbstractMode> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                Codecs.NON_NEGATIVE_INT.optionalFieldOf("abstract_mode", 0).forGetter(AbstractMode::mode)
        ).apply(builder, AbstractMode::new));
    }

    public static final ComponentType<AbstractMode> ABSTRACT_MODE = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(Main.MOD_ID, "abstract_mode"),
            ComponentType.<AbstractMode>builder().codec(AbstractMode.CODEC).build()
    );

    public static void initialize() {
    }
}
