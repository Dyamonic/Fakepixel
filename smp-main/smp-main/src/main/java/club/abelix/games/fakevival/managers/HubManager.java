package club.abelix.games.fakevival.managers;

import com.google.gson.GsonBuilder;
import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.network.SMPServerType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class HubManager {
    private static final String HUB_DATA_ID = "default";
    private final JavaPlugin plugin;
    private final DataManager dataManager;
    private final AtomicBoolean hubSaveRunning = new AtomicBoolean(false);
    private YamlConfiguration hubConfig;
    private Location hubSpawn;

    public HubManager(JavaPlugin plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    public void setup() {
        hubConfig = plugin instanceof Game game
                ? (YamlConfiguration) game.getStaticGameRegistries().systems().hub().config()
                : (YamlConfiguration) new club.abelix.games.fakevival.registries.SystemDefaultsRegistry(plugin).hub().config();
        if (!hubConfig.getBoolean("hub.enabled", true)) return;

        SMPServerType serverType = currentServerType();
        if (serverType != SMPServerType.HUB) {
            if (serverType == SMPServerType.OVERWORLD || serverType == SMPServerType.DUNGEON) {
                plugin.getLogger().info("[SMP] Hub spawn exists in setup data but this is an " + serverType
                        + " server; local hub world is optional.");
            }
            return;
        }

        loadConfiguredWorlds();
        if (hubConfig.getBoolean("hub.import-on-startup", true)) importHubData();
        loadHubSpawn();
    }

    private SMPServerType currentServerType() {
        String value = System.getProperty("SMP_SERVER_TYPE");
        if (value == null || value.isBlank()) value = System.getenv("SMP_SERVER_TYPE");
        return SMPServerType.parse(value);
    }

    private void loadConfiguredWorlds() {
        if (hubConfig.getBoolean("hub.world.auto-load", true)) {
            loadWorld(hubConfig.getString("hub.world.name", "hub"),
                    hubConfig.getString("hub.world.environment", "NORMAL"),
                    hubConfig.getString("hub.world.generator", ""));
        }

        List<String> extraWorlds = hubConfig.getStringList("hub.extra-worlds");
        for (String worldName : extraWorlds) loadWorld(worldName, "NORMAL", "");

        ConfigurationSection dungeons = hubConfig.getConfigurationSection("dungeons");
        if (dungeons == null) return;
        for (String dungeon : dungeons.getKeys(false)) {
            String worldName = dungeons.getString(dungeon + ".world");
            if (worldName != null && !worldName.isBlank()) loadWorld(worldName, "NORMAL", "");
        }
    }

    private void loadWorld(String worldName, String environmentName, String generator) {
        if (worldName == null || worldName.isBlank() || Bukkit.getWorld(worldName) != null) return;
        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (!worldFolder.exists()) {
            plugin.getLogger().info("Hub world folder not found, skipping auto-load: " + worldFolder.getPath());
            return;
        }

        WorldCreator creator = new WorldCreator(worldName);
        creator.environment(parseEnvironment(environmentName));
        if (generator != null && !generator.isBlank()) creator.generator(generator);
        World world = Bukkit.createWorld(creator);
        if (world == null) {
            plugin.getLogger().warning("Could not load hub world: " + worldName);
        } else {
            plugin.getLogger().info("Loaded hub world: " + world.getName());
        }
    }

    private World.Environment parseEnvironment(String value) {
        if (value == null || value.isBlank()) return World.Environment.NORMAL;
        try {
            return World.Environment.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            plugin.getLogger().warning("Invalid hub world environment '" + value + "'. Using NORMAL.");
            return World.Environment.NORMAL;
        }
    }

    private void importHubData() {
        boolean overwrite = hubConfig.getBoolean("hub.overwrite-data", true);
        boolean changed = false;
        changed |= importSection("npcs", overwrite);
        changed |= importSection("dungeons", overwrite);
        changed |= importSection("dungeon-spawn-portal", overwrite);
        changed |= importSection("map-zones", overwrite);
        changed |= importZoneAlias(overwrite);
        changed |= importSection("spawnprotection", overwrite);
        if (dataManager.isMySqlEnabled()) {
            changed |= importHubDataFromDatabase();
        }
        if (changed) {
            dataManager.save();
            plugin.getLogger().info("Imported hub NPC, dungeon, zone, and spawn setup.");
        }
    }

    public boolean exportHubData() {
        plugin.getLogger().info("Hub defaults are Java registry-backed; pod-local YAML export is disabled.");
        return false;
    }

    public boolean saveHubDataAsync(Location spawnLocation, String updatedBy, Consumer<Boolean> callback) {
        if (!dataManager.isMySqlEnabled()) {
            plugin.getLogger().warning("/smp hubsave requires MariaDB persistence. Database is disabled.");
            return false;
        }
        if (!hubSaveRunning.compareAndSet(false, true)) return false;

        if (spawnLocation != null && spawnLocation.getWorld() != null) {
            applyHubSpawn(spawnLocation);
        }

        YamlConfiguration bundle = createHubBundle();
        HubSaveSnapshot snapshot = new HubSaveSnapshot(
                bundle.saveToString(),
                staticRegistryJson(bundle),
                spawnLocation != null && spawnLocation.getWorld() != null ? spawnSnapshot(spawnLocation) : null,
                updatedBy == null ? "" : updatedBy,
                System.currentTimeMillis()
        );

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = false;
            try {
                saveHubSnapshotToDatabase(snapshot);
                success = true;
            } catch (SQLException exception) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save hub data to MariaDB.", exception);
            } finally {
                boolean finalSuccess = success;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    hubSaveRunning.set(false);
                    if (finalSuccess) dataManager.save();
                    if (callback != null) callback.accept(finalSuccess);
                });
            }
        });
        return true;
    }

    private void saveHubSnapshotToDatabase(HubSaveSnapshot snapshot) throws SQLException {
        try (Connection connection = dataManager.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO `smp_hub_data` (`id`, `yaml_data`, `static_registry_json`, `updated_by`, `updated_at`) " +
                        "VALUES (?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE `yaml_data`=VALUES(`yaml_data`), `static_registry_json`=VALUES(`static_registry_json`), " +
                        "`updated_by`=VALUES(`updated_by`), `updated_at`=VALUES(`updated_at`)")) {
            statement.setString(1, HUB_DATA_ID);
            statement.setString(2, snapshot.yaml());
            statement.setString(3, snapshot.staticRegistryJson());
            statement.setString(4, snapshot.updatedBy());
            statement.setLong(5, snapshot.updatedAt());
            statement.executeUpdate();
        }
        if (snapshot.spawn() != null) persistHubSpawnSnapshot(snapshot.spawn(), snapshot.updatedBy(), snapshot.updatedAt());
    }

    public File exportHubStaticRegistryJson() {
        String json = null;
        if (dataManager.isMySqlEnabled()) {
            try (Connection connection = dataManager.getConnection(); PreparedStatement statement = connection.prepareStatement(
                    "SELECT `static_registry_json` FROM `smp_hub_data` WHERE `id`=?")) {
                statement.setString(1, HUB_DATA_ID);
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) json = result.getString("static_registry_json");
                }
            } catch (SQLException exception) {
                plugin.getLogger().warning("Could not read hub static registry export from MariaDB: " + exception.getMessage());
            }
        }
        if (json == null || json.isBlank()) json = staticRegistryJson(createHubBundle());

        File exportFile = new File(plugin.getDataFolder(), "hub-static-registry-export.json");
        try {
            File parent = exportFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            Files.writeString(exportFile.toPath(), json, StandardCharsets.UTF_8);
            return exportFile;
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not write hub static registry export: " + exception.getMessage());
            return null;
        }
    }

    public boolean saveHubSpawn(Location location, String updatedBy) {
        if (location == null || location.getWorld() == null) return false;
        if (!dataManager.isMySqlEnabled()) {
            plugin.getLogger().warning("Hub spawn saving requires MariaDB persistence. Database is disabled; using Java hub default only.");
            return false;
        }
        return persistHubSpawn(location, updatedBy);
    }

    private boolean persistHubSpawn(Location location, String updatedBy) {
        HubSpawnSnapshot snapshot = spawnSnapshot(location);
        if (snapshot == null) return false;
        try {
            persistHubSpawnSnapshot(snapshot, updatedBy == null ? "" : updatedBy, System.currentTimeMillis());
            applyHubSpawn(location);
            return true;
        } catch (SQLException exception) {
            plugin.getLogger().severe("Failed to save hub spawn to MariaDB: " + exception.getMessage());
            return false;
        }
    }

    private void persistHubSpawnSnapshot(HubSpawnSnapshot snapshot, String updatedBy, long updatedAt) throws SQLException {
        try (Connection connection = dataManager.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO `smp_hub_spawns` (`id`, `world`, `x`, `y`, `z`, `yaw`, `pitch`, `updated_by`, `updated_at`) " +
                        "VALUES ('default', ?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "`world`=VALUES(`world`), `x`=VALUES(`x`), `y`=VALUES(`y`), `z`=VALUES(`z`), " +
                        "`yaw`=VALUES(`yaw`), `pitch`=VALUES(`pitch`), `updated_by`=VALUES(`updated_by`), `updated_at`=VALUES(`updated_at`)")) {
            statement.setString(1, snapshot.world());
            statement.setDouble(2, snapshot.x());
            statement.setDouble(3, snapshot.y());
            statement.setDouble(4, snapshot.z());
            statement.setFloat(5, snapshot.yaw());
            statement.setFloat(6, snapshot.pitch());
            statement.setString(7, updatedBy);
            statement.setLong(8, updatedAt);
            statement.executeUpdate();
        }
    }

    public boolean isDatabaseEnabled() {
        return dataManager.isMySqlEnabled();
    }

    public boolean isHubSaveRunning() {
        return hubSaveRunning.get();
    }

    private boolean importHubDataFromDatabase() {
        try (Connection connection = dataManager.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "SELECT `yaml_data` FROM `smp_hub_data` WHERE `id`=?")) {
            statement.setString(1, HUB_DATA_ID);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return false;
                YamlConfiguration bundle = new YamlConfiguration();
                bundle.loadFromString(result.getString("yaml_data"));
                boolean changed = false;
                changed |= importSection(bundle.getConfigurationSection("npcs"), "npcs", true);
                changed |= importSection(bundle.getConfigurationSection("dungeons"), "dungeons", true);
                changed |= importSection(bundle.getConfigurationSection("dungeon-spawn-portal"), "dungeon-spawn-portal", true);
                changed |= importSection(bundle.getConfigurationSection("map-zones"), "map-zones", true);
                changed |= importSection(bundle.getConfigurationSection("spawnprotection"), "spawnprotection", true);
                changed |= importSection(bundle.getConfigurationSection("spawn"), "spawn", true);
                if (changed) plugin.getLogger().info("Loaded hub data bundle from MariaDB.");
                return changed;
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("Could not import hub data bundle from MariaDB: " + exception.getMessage());
            return false;
        }
    }

    public Location getHubSpawn() {
        return hubSpawn == null ? null : hubSpawn.clone();
    }

    private void loadHubSpawn() {
        Location defaultSpawn = defaultHubSpawn();
        if (defaultSpawn != null) {
            applyHubSpawn(defaultSpawn);
        }

        if (!dataManager.isMySqlEnabled()) {
            plugin.getLogger().warning("Database is disabled; using Java default hub spawn. /smp hubsave will not persist.");
            return;
        }

        try (Connection connection = dataManager.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "SELECT `world`, `x`, `y`, `z`, `yaw`, `pitch` FROM `smp_hub_spawns` WHERE `id`='default'")) {
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return;
                World world = Bukkit.getWorld(result.getString("world"));
                if (world == null) {
                    plugin.getLogger().warning("Stored MariaDB hub spawn references unloaded world '" + result.getString("world")
                            + "'. Keeping Java default hub spawn.");
                    return;
                }
                applyHubSpawn(new Location(
                        world,
                        result.getDouble("x"),
                        result.getDouble("y"),
                        result.getDouble("z"),
                        result.getFloat("yaw"),
                        result.getFloat("pitch")
                ));
                plugin.getLogger().info("Loaded hub spawn override from MariaDB.");
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not load hub spawn from MariaDB. Using Java default hub spawn: " + exception.getMessage());
        }
    }

    private Location defaultHubSpawn() {
        ConfigurationSection section = hubConfig.getConfigurationSection("spawn");
        if (section == null) return null;
        World world = Bukkit.getWorld(section.getString("world", ""));
        if (world == null) {
            plugin.getLogger().warning("Java default hub spawn references unloaded world '" + section.getString("world", "") + "'.");
            return null;
        }
        return new Location(
                world,
                section.getDouble("x"),
                section.getDouble("y"),
                section.getDouble("z"),
                (float) section.getDouble("yaw"),
                (float) section.getDouble("pitch")
        );
    }

    private void applyHubSpawn(Location location) {
        if (location == null || location.getWorld() == null) return;
        hubSpawn = location.clone();
        dataManager.getData().set("spawn.world", location.getWorld().getName());
        dataManager.getData().set("spawn.x", location.getX());
        dataManager.getData().set("spawn.y", location.getY());
        dataManager.getData().set("spawn.z", location.getZ());
        dataManager.getData().set("spawn.yaw", location.getYaw());
        dataManager.getData().set("spawn.pitch", location.getPitch());
    }

    private HubSpawnSnapshot spawnSnapshot(Location location) {
        if (location == null || location.getWorld() == null) return null;
        return new HubSpawnSnapshot(
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
    }

    private boolean importSection(String path, boolean overwrite) {
        ConfigurationSection source = hubConfig.getConfigurationSection(path);
        return importSection(source, path, overwrite);
    }

    private boolean importSection(String sourcePath, String targetPath, boolean overwrite) {
        ConfigurationSection source = hubConfig.getConfigurationSection(sourcePath);
        return importSection(source, targetPath, overwrite);
    }

    private boolean importZoneAlias(boolean overwrite) {
        ConfigurationSection source = hubConfig.getConfigurationSection("zones");
        if (!looksLikeZoneData(source)) return false;
        return importSection(source, "map-zones", overwrite);
    }

    private boolean looksLikeZoneData(ConfigurationSection source) {
        if (source == null || source.getKeys(false).isEmpty()) return false;
        for (String key : source.getKeys(false)) {
            ConfigurationSection zone = source.getConfigurationSection(key);
            if (zone != null && (zone.isConfigurationSection("pos1")
                    || zone.isConfigurationSection("pos2")
                    || zone.isConfigurationSection("spawns")
                    || zone.isConfigurationSection("boss-spawns")
                    || zone.contains("type"))) {
                return true;
            }
        }
        return false;
    }

    private boolean importSection(ConfigurationSection source, String targetPath, boolean overwrite) {
        if (source == null) return false;

        if (source.getKeys(false).isEmpty()) return false;

        if (!overwrite && dataManager.getData().isConfigurationSection(targetPath)) {
            return copyMissing(source, dataManager.getData().getConfigurationSection(targetPath));
        }

        dataManager.getData().set(targetPath, null);
        copySection(source, dataManager.getData().createSection(targetPath));
        return true;
    }

    private YamlConfiguration createHubBundle() {
        YamlConfiguration bundle = new YamlConfiguration();
        copyDataSection("npcs", bundle);
        copyDataSection("dungeons", bundle);
        copyDataSection("dungeon-spawn-portal", bundle);
        copyDataSection("map-zones", bundle);
        copyDataSection("spawnprotection", bundle);
        copyDataSection("spawn", bundle);
        return bundle;
    }

    private void copyDataSection(String path, YamlConfiguration bundle) {
        ConfigurationSection source = dataManager.getData().getConfigurationSection(path);
        if (source == null) return;
        copySection(source, bundle.createSection(path));
    }

    private String staticRegistryJson(YamlConfiguration bundle) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (String key : bundle.getKeys(false)) {
            Object value = bundle.get(key);
            if (value instanceof ConfigurationSection section) {
                values.put(key, toMap(section));
            } else {
                values.put(key, value);
            }
        }
        return new GsonBuilder().setPrettyPrinting().create().toJson(values);
    }

    private Map<String, Object> toMap(ConfigurationSection section) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value instanceof ConfigurationSection child) {
                values.put(key, toMap(child));
            } else {
                values.put(key, value);
            }
        }
        return values;
    }

    private void copySection(ConfigurationSection from, ConfigurationSection to) {
        if (from == null || to == null) return;
        for (String key : from.getKeys(false)) {
            Object value = from.get(key);
            if (value instanceof ConfigurationSection section) {
                copySection(section, to.createSection(key));
            } else {
                to.set(key, value);
            }
        }
    }

    private boolean copyMissing(ConfigurationSection from, ConfigurationSection to) {
        if (from == null || to == null) return false;
        boolean changed = false;
        for (String key : from.getKeys(false)) {
            Object value = from.get(key);
            if (value instanceof ConfigurationSection section) {
                ConfigurationSection target = to.getConfigurationSection(key);
                if (target == null) {
                    copySection(section, to.createSection(key));
                    changed = true;
                } else if (copyMissing(section, target)) {
                    changed = true;
                }
            } else if (!to.contains(key)) {
                to.set(key, value);
                changed = true;
            }
        }
        return changed;
    }

    private record HubSaveSnapshot(String yaml, String staticRegistryJson, HubSpawnSnapshot spawn, String updatedBy, long updatedAt) {
    }

    private record HubSpawnSnapshot(String world, double x, double y, double z, float yaw, float pitch) {
    }
}
