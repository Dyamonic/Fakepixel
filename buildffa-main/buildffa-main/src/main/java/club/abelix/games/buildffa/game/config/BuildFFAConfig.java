package club.abelix.games.buildffa.game.config;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.region.Region3Dim;
import club.abelix.api.base.util.location.LocationUtil;
import club.abelix.api.base.util.slime.SlimeWorldUtil;
import lombok.Getter;
import lombok.val;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Locale;

@Getter
public class BuildFFAConfig {
    private final AbstractPlugin plugin;
    private final Collection<WorldConfig> gameWorlds = new HashSet<>();
    private final Map<WorldConfig, String> arenaIds = new IdentityHashMap<>();
    private YamlConfiguration loadedConfiguration;
    private File loadedConfigurationFile;
    private long loadedConfigurationAt;

    public BuildFFAConfig(final AbstractPlugin plugin) {
        this.plugin = plugin;

        createDefaultConfig();
    }

    private void createDefaultConfig() {
        val dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();

        val configFile = getConfigFile();

        if (configFile.exists()) return;

        try (val in = plugin.getResource("config.yml")) {
            if (in != null) Files.copy(in, configFile.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadConfig() {
        val configFile = getConfigFile();

        if (!configFile.exists()) {
            return;
        }

        val yamlConfig = YamlConfiguration.loadConfiguration(configFile);
        loadedConfiguration = yamlConfig;
        loadedConfigurationFile = configFile;
        loadedConfigurationAt = System.currentTimeMillis();
        val worlds = yamlConfig.getConfigurationSection("Worlds");

        gameWorlds.clear();
        arenaIds.clear();

        for (val worldName : worlds.getKeys(false)) {
            val basePath = "Worlds." + worldName + ".";

            WorldConfig arena = new WorldConfig(
                    yamlConfig.getString(basePath + "MapName"),
                    SlimeWorldUtil.loadSLimeWorld(worldName),
                    LocationUtil.stringToLocation(yamlConfig.getString(basePath + "SpawnCoordinates")),

                    new Region3Dim(
                            yamlConfig.getInt(basePath + "PlayZone.MinX"),
                            yamlConfig.getInt(basePath + "PlayZone.MinY"),
                            yamlConfig.getInt(basePath + "PlayZone.MinZ"),
                            yamlConfig.getInt(basePath + "PlayZone.MaxX"),
                            yamlConfig.getInt(basePath + "PlayZone.MaxY"),
                            yamlConfig.getInt(basePath + "PlayZone.MaxZ")
                    ),

                    FakePlayerConfig.create(yamlConfig.getConfigurationSection(basePath + "KitEditor")),
                    FakePlayerConfig.create(yamlConfig.getConfigurationSection(basePath + "ShopNpc")),
                    FakePlayerConfig.create(yamlConfig.getConfigurationSection(basePath + "DivisionNpc")),
                    FakePlayerConfig.create(yamlConfig.getConfigurationSection(basePath + "ExchangerNpc")),
                    FakePlayerConfig.create(yamlConfig.getConfigurationSection(basePath + "LayoutChangeNpc")),
                    FakePlayerConfig.create(yamlConfig.getConfigurationSection(basePath + "QuestsNpc") != null
                            ? yamlConfig.getConfigurationSection(basePath + "QuestsNpc")
                            : yamlConfig.getConfigurationSection(basePath + "ShopNpc")),
                    FakePlayerConfig.create(yamlConfig.getConfigurationSection(basePath + "CosmeticNpc") != null
                            ? yamlConfig.getConfigurationSection(basePath + "CosmeticNpc")
                            : yamlConfig.getConfigurationSection(basePath + "BuffsNpc") != null
                            ? yamlConfig.getConfigurationSection(basePath + "BuffsNpc")
                            : yamlConfig.getConfigurationSection(basePath + "ShopNpc")),

                    LocationUtil.stringToLocation(yamlConfig.getString(basePath + "KillStreakLeaderboard")),
                    LocationUtil.stringToLocation(yamlConfig.getString(basePath + "KillsLeaderboard")),
                    LocationUtil.stringToLocation(yamlConfig.getString(basePath + "RatingLeaderboard")),

                    LocationUtil.stringToLocation(yamlConfig.getString(basePath + "RatingHologram")),

                    yamlConfig.getInt(basePath + "player_move_y_threshold_kit"),
                    yamlConfig.getInt(basePath + "setup_player_inventory_min_height"),
                    yamlConfig.getInt(basePath + "unvisible_players_level"),
                    yamlConfig.getInt(basePath + "death_level")
            );
            gameWorlds.add(arena);
            arenaIds.put(arena, normalizeArenaId(worldName));
        }
    }

    public File getConfigFile() {
        return new File(plugin.getDataFolder(), "config.yml").getAbsoluteFile();
    }

    public String getArenaId(WorldConfig arena) {
        return arenaIds.get(arena);
    }

    public static String normalizeArenaId(String id) {
        return id == null ? null : id.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    public boolean isEventEnabled(String eventId, boolean defaultValue) {
        val configFile = getConfigFile();
        if (!configFile.exists()) return defaultValue;
        val yamlConfig = YamlConfiguration.loadConfiguration(configFile);
        String path = "Events." + eventId + ".Enabled";
        return yamlConfig.contains(path) ? yamlConfig.getBoolean(path) : defaultValue;
    }

    public boolean saveEventEnabled(String eventId, boolean enabled) {
        val configFile = getConfigFile();
        val yamlConfig = YamlConfiguration.loadConfiguration(configFile);
        yamlConfig.set("Events." + eventId + ".Enabled", enabled);
        try {
            yamlConfig.save(configFile);
            return true;
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save BuildFFA event availability: " + exception.getMessage());
            return false;
        }
    }
}
