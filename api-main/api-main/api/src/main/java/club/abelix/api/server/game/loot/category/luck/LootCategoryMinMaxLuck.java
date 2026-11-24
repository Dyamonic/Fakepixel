package club.abelix.api.server.game.loot.category.luck;

import org.bukkit.configuration.ConfigurationSection;

/**
 * @author Unidentified Person
 */
public class LootCategoryMinMaxLuck implements LootCategoryLuck {

    private final int min, max;

    public LootCategoryMinMaxLuck(ConfigurationSection section) {
        this.min = section.getInt("Min");
        this.max = section.getInt("Max");
    }

    @Override
    public int getCount() {
        return max == min ? min : (int) (Math.random() * (max - min) + min);
    }
}
