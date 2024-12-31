package jacksunderscoreusername.trinkets.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import jacksunderscoreusername.trinkets.Main;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;

public class PaperComponent {
    public record CooldownData(String tooltip, String contents) {
        public static final Codec<CooldownData> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                Codecs.NON_EMPTY_STRING.fieldOf("tooltip").forGetter(CooldownData::tooltip),
                Codecs.NON_EMPTY_STRING.fieldOf("contents").forGetter(CooldownData::contents)
        ).apply(builder, CooldownData::new));
    }

    public static final ComponentType<CooldownData> PAPER = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(Main.MOD_ID, "paper"),
            ComponentType.<CooldownData>builder().codec(CooldownData.CODEC).build()
    );

    public static void initialize(){}
}
