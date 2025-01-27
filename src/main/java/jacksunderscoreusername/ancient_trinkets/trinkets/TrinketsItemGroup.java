package jacksunderscoreusername.ancient_trinkets.trinkets;

import jacksunderscoreusername.ancient_trinkets.Main;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class TrinketsItemGroup {

    public static final ItemGroup TRINKETS_GROUP = FabricItemGroup.builder()
            .icon(() -> new ItemStack(Trinkets.ACTIVATED_ECHO_SHARD))
            .displayName(Text.translatable("itemGroup." + Main.MOD_ID + ".trinket_group"))
            .entries((context, entries) -> {
                for (var trinket : Trinkets.allTrinkets) {
                    entries.add(trinket);
                }
                entries.add(Trinkets.UNCOMMON_TRINKET_DUST);
                entries.add(Trinkets.RARE_TRINKET_DUST);
                entries.add(Trinkets.EPIC_TRINKET_DUST);
            })
            .build();

    public static void initialize() {
        Registry.register(Registries.ITEM_GROUP, Identifier.of(Main.MOD_ID, "trinket_group"), TRINKETS_GROUP);
    }
}
