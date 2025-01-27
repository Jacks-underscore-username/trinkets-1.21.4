package jacksunderscoreusername.ancient_trinkets.trinkets;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import jacksunderscoreusername.ancient_trinkets.Main;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;

public class ChargesDataComponent {
    public record Charges(int charges) {
        public static final Codec<Charges> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                Codecs.NON_NEGATIVE_INT.optionalFieldOf("charges", 0).forGetter(Charges::charges)
        ).apply(builder, Charges::new));
    }

    public static final ComponentType<Charges> CHARGES = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(Main.MOD_ID, "charges"),
            ComponentType.<Charges>builder().codec(Charges.CODEC).build()
    );

    public static void initialize(){}
}
