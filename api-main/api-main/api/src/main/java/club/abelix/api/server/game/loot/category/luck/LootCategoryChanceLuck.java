package club.abelix.api.server.game.loot.category.luck;

import org.bukkit.configuration.ConfigurationSection;

/**
 * @author Unidentified Person
 */
public class LootCategoryChanceLuck implements LootCategoryLuck {

    private final double chance;

    public LootCategoryChanceLuck(ConfigurationSection section) {
        this.chance = section.getDouble("Chance", 100) / 100D;
    }

    @Override
    public int getCount() {
        int intChance = (int) chance;

        return chance != intChance
                ? intChance + (Math.random() <= (chance - intChance) ? 1 : 0)
                : intChance;
    }

}
