package jacksunderscoreusername.ancient_trinkets.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ActionResult;

public interface LivingEntityDeathEvent {
    Event<LivingEntityDeathEvent> EVENT = EventFactory.createArrayBacked(LivingEntityDeathEvent.class,
            (listeners) -> (entity) -> {
                for (LivingEntityDeathEvent listener : listeners) {
                    ActionResult result = listener.interact(entity);

                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }
                return ActionResult.PASS;
            });

    ActionResult interact(LivingEntity entity);
}
