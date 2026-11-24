package club.abelix.games.fakevival.registries;

import club.abelix.games.fakevival.config.StaticConfigDefaults;
import club.abelix.games.fakevival.registries.definitions.StoreProductDefinition;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class StoreRegistry extends StaticConfigRegistry {
    private Map<String, StoreProductDefinition> products = Map.of();
    private Map<String, List<StoreProductDefinition>> byCategory = Map.of();

    public StoreRegistry(JavaPlugin plugin) {
        super(StaticConfigDefaults.STORE);
        reload(plugin);
    }

    @Override
    protected void rebuild() {
        Map<String, StoreProductDefinition> nextProducts = new LinkedHashMap<>();
        Map<String, List<StoreProductDefinition>> nextByCategory = new LinkedHashMap<>();
        collectProducts("store.products", "store", nextProducts, nextByCategory);
        collectProducts("products", "store", nextProducts, nextByCategory);
        products = Map.copyOf(nextProducts);
        byCategory = immutableGrouped(nextByCategory);
    }

    private void collectProducts(String path, String fallbackCategory, Map<String, StoreProductDefinition> nextProducts,
                                 Map<String, List<StoreProductDefinition>> nextByCategory) {
        ConfigurationSection root = section(path);
        if (root == null) return;
        for (String productId : root.getKeys(false)) {
            ConfigurationSection product = root.getConfigurationSection(productId);
            if (product == null) continue;
            String category = category(product, fallbackCategory);
            StoreProductDefinition definition = new StoreProductDefinition(
                    productId,
                    category,
                    displayName(product, productId),
                    product.getBoolean("enabled", true),
                    data(product)
            );
            nextProducts.putIfAbsent(productId, definition);
            nextByCategory.computeIfAbsent(category, ignored -> new ArrayList<>()).add(definition);
        }
    }

    public Optional<StoreProductDefinition> get(String id) {
        return Optional.ofNullable(products.get(id));
    }

    public List<StoreProductDefinition> all() {
        return List.copyOf(products.values());
    }

    public boolean contains(String id) {
        return products.containsKey(id);
    }

    public List<StoreProductDefinition> byCategory(String category) {
        return byCategory.getOrDefault(category, List.of());
    }
}
