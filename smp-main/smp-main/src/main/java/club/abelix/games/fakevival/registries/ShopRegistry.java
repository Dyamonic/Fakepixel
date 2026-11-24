package club.abelix.games.fakevival.registries;

import club.abelix.games.fakevival.config.StaticConfigDefaults;
import club.abelix.games.fakevival.registries.definitions.ShopItemDefinition;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ShopRegistry extends StaticConfigRegistry {
    private Map<String, ShopItemDefinition> items = Map.of();
    private Map<String, List<ShopItemDefinition>> byShop = Map.of();
    private Map<String, List<ShopItemDefinition>> byCategory = Map.of();

    public ShopRegistry(JavaPlugin plugin) {
        super(StaticConfigDefaults.SHOPS);
        reload(plugin);
    }

    @Override
    protected void rebuild() {
        Map<String, ShopItemDefinition> nextItems = new LinkedHashMap<>();
        Map<String, List<ShopItemDefinition>> nextByShop = new LinkedHashMap<>();
        Map<String, List<ShopItemDefinition>> nextByCategory = new LinkedHashMap<>();
        for (String shopId : shopIds()) {
            for (String itemId : itemIds(shopId)) {
                ConfigurationSection section = item(shopId, itemId);
                if (section == null) continue;
                String id = shopId + "." + itemId;
                String category = category(section, shopId);
                ShopItemDefinition definition = new ShopItemDefinition(
                        id,
                        shopId,
                        displayName(section, itemId),
                        category,
                        section.getBoolean("enabled", true),
                        data(section)
                );
                nextItems.put(id, definition);
                nextByShop.computeIfAbsent(shopId, ignored -> new ArrayList<>()).add(definition);
                nextByCategory.computeIfAbsent(category, ignored -> new ArrayList<>()).add(definition);
            }
        }
        items = Map.copyOf(nextItems);
        byShop = immutableGrouped(nextByShop);
        byCategory = immutableGrouped(nextByCategory);
    }

    public Set<String> shopIds() {
        return keys("shops");
    }

    public Set<String> itemIds(String shopId) {
        return keys("shops." + shopId + ".items");
    }

    public ConfigurationSection item(String shopId, String itemId) {
        return section("shops." + shopId + ".items." + itemId);
    }

    public Optional<ShopItemDefinition> get(String id) {
        return Optional.ofNullable(items.get(id));
    }

    public List<ShopItemDefinition> all() {
        return List.copyOf(items.values());
    }

    public boolean contains(String id) {
        return items.containsKey(id);
    }

    public List<ShopItemDefinition> byShop(String shopId) {
        return byShop.getOrDefault(shopId, List.of());
    }

    public List<ShopItemDefinition> byCategory(String category) {
        return byCategory.getOrDefault(category, List.of());
    }
}
