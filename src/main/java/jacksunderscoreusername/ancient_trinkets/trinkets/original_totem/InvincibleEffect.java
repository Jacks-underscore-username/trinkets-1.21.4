package jacksunderscoreusername.ancient_trinkets.trinkets.original_totem;

import jacksunderscoreusername.ancient_trinkets.Main;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

public class InvincibleEffect extends StatusEffect {
    protected InvincibleEffect(StatusEffectCategory category, int color) {
        super(category, color);
    }

    public static RegistryEntry<StatusEffect> INVINCIBLE = register(
            "invincible", new InvincibleEffect(StatusEffectCategory.BENEFICIAL, 0xff9966)
    );

    private static RegistryEntry<StatusEffect> register(String id, StatusEffect statusEffect) {
        return Registry.registerReference(Registries.STATUS_EFFECT, Identifier.of(Main.MOD_ID, id), statusEffect);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyUpdateEffect(ServerWorld world, LivingEntity entity, int amplifier) {
        return super.applyUpdateEffect(world, entity, amplifier);
    }

    public static void initialize() {
    }

    ;
}
