package club.abelix.games.fakevival.registries;

import club.abelix.games.fakevival.config.StaticConfigDefaults;
import club.abelix.games.fakevival.registries.definitions.CollectionDefinition;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class CollectionRegistry extends StaticConfigRegistry {
    private Map<String, CollectionDefinition> collections = Map.of();

    public CollectionRegistry(JavaPlugin plugin) {
        super(StaticConfigDefaults.COLLECTIONS);
        reload(plugin);
    }

    @Override
    protected void rebuild() {
        Map<String, CollectionDefinition> nextCollections = new LinkedHashMap<>();
        for (String collectionId : collectionIds()) {
            ConfigurationSection section = section("collections." + collectionId);
            if (section == null) continue;
            nextCollections.put(collectionId, new CollectionDefinition(
                    collectionId,
                    displayName(section, collectionId),
                    section.getBoolean("enabled", true),
                    data(section)
            ));
        }
        collections = Map.copyOf(nextCollections);
    }

    public Set<String> collectionIds() {
        return keys("collections");
    }

    public Optional<CollectionDefinition> get(String id) {
        return Optional.ofNullable(collections.get(id));
    }

    public List<CollectionDefinition> all() {
        return List.copyOf(collections.values());
    }

    public boolean contains(String id) {
        return collections.containsKey(id);
    }
}
