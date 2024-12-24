package jacksunderscoreusername.trinkets;

import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;

public class Trinket extends Item {
    public Trinket(Settings settings) {
        super(settings);
    }

    public String getId() {
        throw new RuntimeException("This function should never be called");
    }

    public static Settings getSettings() {
        return new Settings();
    }

    public void markCreated() {
        Main.state.data.createdTrinkets.add(this.getId());
        Main.LOGGER.info("Trinket \"{}\" was created", this.getId());
    }

    public void markRemoved() {
        Main.state.data.createdTrinkets.remove(this.getId());
        Main.LOGGER.info("Trinket \"{}\" was removed", this.getId());
    }

    @Override
    public void onItemEntityDestroyed(ItemEntity entity) {
        this.markRemoved();
        super.onItemEntityDestroyed(entity);
    }

    public void initialize() {
        Main.LOGGER.warn("Trinket type \"" + this.getId() + "\" has no initializeCreationHandler");
    }
}
