package club.abelix.api.server.game.loot.category;

import club.abelix.api.base.util.config.ConfigUtil;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.loot.category.luck.LootCategoryChanceLuck;
import club.abelix.api.server.game.loot.category.luck.LootCategoryLuck;
import club.abelix.api.server.game.loot.category.luck.LootCategoryMinMaxLuck;
import club.abelix.api.server.game.loot.predicate.LootPredicate;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Unidentified Person
 */
public class LootCategory {

    private final String name;

    private final Map<String, LootCategoryLuck> luck;
    private final List<LootCategoryItem> items;

    private final LootPredicate predicate;

    public LootCategory(ConfigurationSection section) {
        name = section.getName();

        ConfigurationSection sLocations = section.getConfigurationSection("Locations");

        luck = sLocations.getKeys(false).stream()
                .map(sLocations::getConfigurationSection)
                .collect(Collectors.toMap(
                        ConfigurationSection::getName,
                        v -> v.contains("Chance")
                                ? new LootCategoryChanceLuck(v)
                                : new LootCategoryMinMaxLuck(v)
                ));

        items = ConfigUtil.getList(section, "Items").stream()
                .map(LootCategoryItem::new)
                .collect(Collectors.toList());

        predicate = section.contains("Predicate")
                ? LootPredicate.parse(section.getString("Predicate"))
                : null;
    }

    public LootCategoryLuck getLuck(String location) {
        return luck.get(location);
    }

    public String getName() {
        return name;
    }

    public List<LootCategoryItem> getItems() {
        return items;
    }

    public boolean test(Game game) {
        return predicate == null || predicate.eval(game);
    }
}
