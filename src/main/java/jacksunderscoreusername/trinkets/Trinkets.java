package jacksunderscoreusername.trinkets;

import jacksunderscoreusername.trinkets.trinkets.activated_echo_shard.ActivatedEchoShard;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public class Trinkets {

    public static final Trinket ACTIVATED_ECHO_SHARD = register(ActivatedEchoShard.id, ActivatedEchoShard::new, ActivatedEchoShard.getSettings());

    public static final Trinket[] AllTrinkets = {ACTIVATED_ECHO_SHARD};

    public static Trinket register(String id, Function<Item.Settings, Item> factory, Item.Settings settings) {

        final RegistryKey<Item> registryKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Main.MOD_ID, id));
        return (Trinket) Items.register(registryKey, factory, settings);
    }

    public static void initialize() {
        TrinketsItemGroup.initialize();
        for (var trinket : AllTrinkets) {
            trinket.initialize();
        }
    }
}
