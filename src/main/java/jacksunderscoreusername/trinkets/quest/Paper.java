package jacksunderscoreusername.trinkets.quest;

import jacksunderscoreusername.trinkets.Main;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class Paper extends Item {

    public Paper(Settings settings) {
        super(settings);
    }

    public static Paper PAPER = (Paper) Items.register(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Main.MOD_ID, "paper")), Paper::new, new Item.Settings().maxCount(1));

    public static void initialize() {
        PaperComponent.initialize();
    }
}
