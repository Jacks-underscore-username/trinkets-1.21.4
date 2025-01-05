package jacksunderscoreusername.trinkets.trinkets.soul_lamp;

import jacksunderscoreusername.trinkets.events.LivingEntityDeathEvent;
import jacksunderscoreusername.trinkets.Main;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;

public class CursedEffect extends StatusEffect {
    protected CursedEffect(StatusEffectCategory category, int color) {
        super(category, color);
    }

    public static RegistryEntry<StatusEffect> CURSED = register(
            "cursed", new CursedEffect(StatusEffectCategory.HARMFUL, 0x151515)
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
        LivingEntityDeathEvent.EVENT.register((entity) -> {
            if (entity.getWorld().isClient) {
                return ActionResult.PASS;
            }
            ServerWorld world = (ServerWorld) entity.getWorld();
            StatusEffectInstance effect = entity.getStatusEffect(CURSED);
            if (effect != null) {
                for (var i = 0; i <= effect.getAmplifier(); i++) {
                    Ghost.GHOST.spawn(world, entity.getBlockPos(), SpawnReason.MOB_SUMMONED);
                    for (var j = 0; j < 10; j++) {
                        world.addParticle(
                                ParticleTypes.SOUL,
                                entity.getParticleX(0.5),
                                entity.getRandomBodyY(),
                                entity.getParticleZ(0.5),
                                (world.random.nextDouble() - 0.5) * 2.0,
                                -world.random.nextDouble(),
                                (world.random.nextDouble() - 0.5) * 2.0
                        );
                    }
                }
                world.playSound(null, entity.getBlockPos(), SoundEvents.PARTICLE_SOUL_ESCAPE.value(), SoundCategory.PLAYERS, 5, 0.5F);
            }
            return ActionResult.PASS;
        });
    }
}
