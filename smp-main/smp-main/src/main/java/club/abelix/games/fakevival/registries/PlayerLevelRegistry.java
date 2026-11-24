package club.abelix.games.fakevival.registries;

import club.abelix.games.fakevival.config.StaticConfigDefaults;
import club.abelix.games.fakevival.registries.definitions.PlayerLevelDefinition;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class PlayerLevelRegistry extends StaticConfigRegistry {
    private Map<String, PlayerLevelDefinition> levels = Map.of();

    public PlayerLevelRegistry(JavaPlugin plugin) {
        super(StaticConfigDefaults.PLAYER_LEVELS);
        reload(plugin);
    }

    @Override
    protected void rebuild() {
        Map<String, PlayerLevelDefinition> nextLevels = new LinkedHashMap<>();
        for (String levelId : levelIds()) {
            ConfigurationSection section = section("player-levels.levels." + levelId);
            if (section == null) continue;
            int level = section.getInt("level", parseLevel(levelId));
            nextLevels.put(levelId, new PlayerLevelDefinition(
                    levelId,
                    level,
                    displayName(section, levelId),
                    section.getBoolean("enabled", true),
                    data(section)
            ));
        }
        levels = Map.copyOf(nextLevels);
    }

    public Set<String> levelIds() {
        return keys("player-levels.levels");
    }

    public Optional<PlayerLevelDefinition> get(String id) {
        return Optional.ofNullable(levels.get(id));
    }

    public List<PlayerLevelDefinition> all() {
        return List.copyOf(levels.values());
    }

    public boolean contains(String id) {
        return levels.containsKey(id);
    }

    private int parseLevel(String id) {
        try {
            return Integer.parseInt(id.replace("level_", ""));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
