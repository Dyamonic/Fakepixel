package club.abelix.api.server.game.loot.category;

import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.loot.predicate.LootPredicate;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

/**
 * @author Unidentified Person
 */
public class LootCategoryItem {

    private final ItemStack itemStack;
    private final double chance;
    private final LootPredicate predicate;

    public LootCategoryItem(ConfigurationSection section) {
        this.itemStack = ItemUtil.parseItem(section);
        this.chance = section.getDouble("chance", 100) / 100;

        this.predicate = section.contains("predicate")
                ? LootPredicate.parse(section.getString("predicate"))
                : null;
    }

    @Override
    public String toString() {
        return itemStack.toString() + " % " + chance;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public double getChance() {
        return chance;
    }

    public LootPredicate getPredicate() {
        return predicate;
    }

    public boolean test(Game game) {
        return predicate == null || predicate.eval(game);
    }
}
