package club.abelix.games.fakevival.registries;

import club.abelix.games.fakevival.config.StaticConfigDefaults;
import club.abelix.games.fakevival.registries.definitions.CompanionDefinition;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class CompanionRegistry extends StaticConfigRegistry {
    private Map<String, CompanionDefinition> companions = Map.of();

    public CompanionRegistry(JavaPlugin plugin) {
        super(StaticConfigDefaults.COMPANIONS);
        reload(plugin);
    }

    @Override
    protected void rebuild() {
        Map<String, CompanionDefinition> nextCompanions = new LinkedHashMap<>();
        for (String companionId : companionIds()) {
            ConfigurationSection section = section("companions." + companionId);
            if (section == null) continue;
            nextCompanions.put(companionId, new CompanionDefinition(
                    companionId,
                    displayName(section, companionId),
                    section.getBoolean("enabled", true),
                    data(section)
            ));
        }
        companions = Map.copyOf(nextCompanions);
    }

    public Set<String> companionIds() {
        return keys("companions");
    }

    public Optional<CompanionDefinition> get(String id) {
        return Optional.ofNullable(companions.get(id));
    }

    public List<CompanionDefinition> all() {
        return List.copyOf(companions.values());
    }

    public boolean contains(String id) {
        return companions.containsKey(id);
    }
}
