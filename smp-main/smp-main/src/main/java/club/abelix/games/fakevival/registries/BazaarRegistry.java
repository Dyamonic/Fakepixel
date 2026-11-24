package club.abelix.games.fakevival.registries;

import club.abelix.games.fakevival.config.StaticConfigDefaults;
import club.abelix.games.fakevival.registries.definitions.BazaarItemDefinition;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class BazaarRegistry extends StaticConfigRegistry {
    private Map<String, BazaarItemDefinition> items = Map.of();
    private Map<String, List<BazaarItemDefinition>> byCategory = Map.of();

    public BazaarRegistry(JavaPlugin plugin) {
        super(StaticConfigDefaults.BAZAAR);
        reload(plugin);
    }

    @Override
    protected void rebuild() {
        Map<String, BazaarItemDefinition> nextItems = new LinkedHashMap<>();
        Map<String, List<BazaarItemDefinition>> nextByCategory = new LinkedHashMap<>();
        for (String itemId : itemIds()) {
            ConfigurationSection section = item(itemId);
            if (section == null) continue;
            String category = category(section, "default");
            BazaarItemDefinition definition = new BazaarItemDefinition(
                    itemId,
                    displayName(section, itemId),
                    category,
                    section.getBoolean("enabled", true),
                    data(section)
            );
            nextItems.put(itemId, definition);
            nextByCategory.computeIfAbsent(category, ignored -> new ArrayList<>()).add(definition);
        }
        items = Map.copyOf(nextItems);
        byCategory = immutableGrouped(nextByCategory);
    }

    public Set<String> categoryIds() {
        return keys("bazaar.categories");
    }

    public Set<String> itemIds() {
        return keys("bazaar.items");
    }

    public ConfigurationSection item(String itemId) {
        return section("bazaar.items." + itemId);
    }

    public Optional<BazaarItemDefinition> get(String id) {
        return Optional.ofNullable(items.get(id));
    }

    public List<BazaarItemDefinition> all() {
        return List.copyOf(items.values());
    }

    public boolean contains(String id) {
        return items.containsKey(id);
    }

    public List<BazaarItemDefinition> byCategory(String category) {
        return byCategory.getOrDefault(category, List.of());
    }
}
