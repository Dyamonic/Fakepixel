package me.outerspace.thepit.managers;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.registry.definition.ArenaDefinition;
import me.outerspace.thepit.registry.definition.StoredLocation;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class PitYamlManager {
    private final ThePitPlugin plugin;
    private final File file;
    private volatile YamlConfiguration configuration = new YamlConfiguration();
    private volatile Map<String, ArenaDefinition> arenas = Collections.emptyMap();
    private volatile Map<String, String> spawnSources = Collections.emptyMap();
    private volatile Map<String, ArenaFeatureConfiguration.LaunchPads> launchPads = Collections.emptyMap();
    private volatile Map<String, ArenaFeatureConfiguration.CoinSpawns> coinSpawns = Collections.emptyMap();
    private volatile Map<String, ArenaFeatureConfiguration.CarePackages> carePackages = Collections.emptyMap();
    private volatile Map<String, RagePitDefinition> ragePits = Collections.emptyMap();

    public PitYamlManager(ThePitPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "arenas.yml");
    }

    public boolean initialize() {
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            plugin.saveResource("arenas.yml", false);
        }
        return reload();
    }

    public synchronized boolean reload() {
        try {
            YamlConfiguration loaded = new YamlConfiguration();
            loaded.load(file);
            ConfigurationSection root = loaded.getConfigurationSection("arenas");
            if (root == null) throw new InvalidConfigurationException("Missing arenas root section");
            boolean migrated = normalizeArenaKeys(loaded, root);
            root = loaded.getConfigurationSection("arenas");
            Map<String, ArenaDefinition> temporary = new LinkedHashMap<>();
            Map<String, String> sources = new LinkedHashMap<>();
            Map<String, ArenaFeatureConfiguration.LaunchPads> temporaryPads = new LinkedHashMap<>();
            Map<String, ArenaFeatureConfiguration.CoinSpawns> temporaryCoins = new LinkedHashMap<>();
            Map<String, ArenaFeatureConfiguration.CarePackages> temporaryCarePackages = new LinkedHashMap<>();
            Map<String, RagePitDefinition> temporaryRagePits = new LinkedHashMap<>();
            for (String rawId : root.getKeys(false)) {
                String id = normalizeArenaId(rawId);
                if (id.isEmpty()) throw new InvalidConfigurationException("Invalid empty arena ID under arenas");
                if (temporary.containsKey(id)) throw new InvalidConfigurationException("Duplicate normalized arena ID: " + id);
                ConfigurationSection section = root.getConfigurationSection(rawId);
                if (section == null) throw new InvalidConfigurationException("Invalid arena section: arenas." + rawId);
                String world = requiredString(section, id, "world");
                String displayName = section.getString("display-name", id);
                String type = section.getString("type", "normal");
                boolean enabled = section.getBoolean("enabled", true);
                LocationRead spawnRead = readSpawn(loaded, section, id);
                migrated |= spawnRead.migrated();
                migrated |= migrateFeatureLayout(loaded, section, id);
                section = loaded.getConfigurationSection("arenas." + id);
                StoredLocation spawnProtectionPos1 = readOptionalLocation(section, "spawn-protection.pos1", id);
                StoredLocation spawnProtectionPos2 = readOptionalLocation(section, "spawn-protection.pos2", id);
                StoredLocation pvpPos1 = readOptionalLocation(section, "pvp-arena.pos1", id);
                StoredLocation pvpPos2 = readOptionalLocation(section, "pvp-arena.pos2", id);
                ArenaDefinition definition = new ArenaDefinition(
                        id,
                        enabled,
                        displayName,
                        world,
                        type,
                        spawnRead.location(),
                        spawnProtectionPos1,
                        spawnProtectionPos2,
                        pvpPos1,
                        pvpPos2
                );
                temporary.put(id, definition);
                sources.put(id, spawnRead.source());
                temporaryPads.put(id, readLaunchPads(section, id, world));
                temporaryCoins.put(id, readCoinSpawns(section, id, world));
                temporaryCarePackages.put(id, readCarePackages(section, id, world));
                RagePitRead ragePit = readRagePit(loaded, section, id, world);
                migrated |= ragePit.migrated();
                temporaryRagePits.put(id, ragePit.definition());
                if (!ragePit.definition().valid() && !ragePit.definition().error().startsWith("disabled YAML path")) plugin.getLogger().warning("Rage Pit configuration rejected for arena '" + id + "': " + ragePit.definition().error());
            }
            if (temporary.isEmpty()) throw new InvalidConfigurationException("No arenas were defined under arenas");
            if (migrated) saveAtomically(loaded);
            configuration = loaded;
            arenas = Collections.unmodifiableMap(new LinkedHashMap<>(temporary));
            spawnSources = Collections.unmodifiableMap(new LinkedHashMap<>(sources));
            launchPads = Collections.unmodifiableMap(new LinkedHashMap<>(temporaryPads));
            coinSpawns = Collections.unmodifiableMap(new LinkedHashMap<>(temporaryCoins));
            carePackages = Collections.unmodifiableMap(new LinkedHashMap<>(temporaryCarePackages));
            ragePits = Collections.unmodifiableMap(new LinkedHashMap<>(temporaryRagePits));
            for (ArenaDefinition arena : temporary.values()) logLoadedSpawn(arena);
            return true;
        } catch (IOException | InvalidConfigurationException | RuntimeException exception) {
            plugin.getLogger().severe("Could not load arenas.yml; keeping the previous arena cache: " + exception.getMessage());
            return false;
        }
    }

    public synchronized boolean saveSpawn(String requestedArenaId, StoredLocation location) {
        String id = normalizeArenaId(requestedArenaId);
        if (location == null || !arenas.containsKey(id)) return false;
        try {
            YamlConfiguration updated = loadForWrite();
            String base = "arenas." + id + ".spawn";
            updated.set(base, null);
            writeLocation(updated, base, location);
            saveAtomically(updated);
            return reload() && arenas.get(id) != null && arenas.get(id).spawn() != null;
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger().severe("Could not save spawn for arena '" + id + "' to arenas.yml: " + exception.getMessage());
            return false;
        }
    }

    public String worldTemplate(String requestedArenaId) {
        String id = normalizeArenaId(requestedArenaId);
        ArenaDefinition arena = arenas.get(id);
        return configuration.getString("arenas." + id + ".world-template", arena == null ? id : arena.worldName());
    }

    public synchronized boolean saveRegionPoint(String requestedArenaId, String region, String point, StoredLocation location) {
        String id = normalizeArenaId(requestedArenaId);
        if (location == null || !arenas.containsKey(id)) return false;
        try {
            YamlConfiguration updated = loadForWrite();
            writeLocation(updated, "arenas." + id + "." + region + "." + point, location);
            saveAtomically(updated);
            return reload();
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger().severe("Could not save region point for arena '" + id + "' to arenas.yml: " + exception.getMessage());
            return false;
        }
    }

    public synchronized boolean saveLaunchPad(String requestedArenaId, String name, Location location,
                                              ArenaFeatureConfiguration.LaunchPadDefaults values) {
        String arenaId = normalizeArenaId(requestedArenaId);
        String stableId = normalizeStableId(name);
        if (location == null || location.getWorld() == null || values == null || stableId.isEmpty() || !arenas.containsKey(arenaId)) return false;
        try {
            YamlConfiguration updated = loadForWrite();
            String base = "arenas." + arenaId + ".launch-pads.locations." + stableId;
            updated.set(base + ".world", location.getWorld().getName());
            updated.set(base + ".x", location.getBlockX());
            updated.set(base + ".y", location.getBlockY());
            updated.set(base + ".z", location.getBlockZ());
            updated.set(base + ".horizontal-power", values.horizontalPower());
            updated.set(base + ".vertical-power", values.verticalPower());
            updated.set(base + ".cooldown-millis", values.cooldownMillis());
            updated.set(base + ".fall-protection-seconds", values.fallProtectionSeconds());
            updated.set(base + ".particle", values.particle());
            updated.set(base + ".sound", values.sound());
            saveAtomically(updated);
            return reload() && launchPads(arenaId).locations().containsKey(stableId);
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger().severe("Could not save launch pad '" + stableId + "' for arena '" + arenaId + "': " + exception.getMessage());
            return false;
        }
    }

    public synchronized boolean removeLaunchPad(String requestedArenaId, String name) {
        return removeFeatureLocation(requestedArenaId, "launch-pads", name);
    }

    public synchronized boolean saveCoinSpawn(String requestedArenaId, String name, Location location, int reward, int respawnSeconds) {
        String arenaId = normalizeArenaId(requestedArenaId);
        String stableId = normalizeStableId(name);
        if (location == null || location.getWorld() == null || stableId.isEmpty() || !arenas.containsKey(arenaId)) return false;
        try {
            YamlConfiguration updated = loadForWrite();
            String base = "arenas." + arenaId + ".coin-spawns.locations." + stableId;
            updated.set(base + ".world", location.getWorld().getName());
            updated.set(base + ".x", location.getX());
            updated.set(base + ".y", location.getY());
            updated.set(base + ".z", location.getZ());
            updated.set(base + ".yaw", location.getYaw());
            updated.set(base + ".pitch", location.getPitch());
            updated.set(base + ".reward", Math.max(0, reward));
            updated.set(base + ".respawn-seconds", Math.max(0, respawnSeconds));
            saveAtomically(updated);
            return reload() && coinSpawns(arenaId).locations().containsKey(stableId);
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger().severe("Could not save coin spawn '" + stableId + "' for arena '" + arenaId + "': " + exception.getMessage());
            return false;
        }
    }

    public synchronized boolean removeCoinSpawn(String requestedArenaId, String name) {
        return removeFeatureLocation(requestedArenaId, "coin-spawns", name);
    }

    public synchronized boolean saveCarePackage(String requestedArenaId, String name, Location location, double dropHeight) {
        String arenaId = normalizeArenaId(requestedArenaId);
        String stableId = normalizeStableId(name);
        if (location == null || location.getWorld() == null || stableId.isEmpty() || !arenas.containsKey(arenaId)) return false;
        ArenaDefinition arena = arenas.get(arenaId);
        if (!arena.worldName().equalsIgnoreCase(location.getWorld().getName())) return false;
        try {
            YamlConfiguration updated = loadForWrite();
            String base = "arenas." + arenaId + ".care-packages.locations." + stableId;
            updated.set("arenas." + arenaId + ".care-packages.enabled", true);
            updated.set(base + ".world", location.getWorld().getName());
            updated.set(base + ".x", location.getBlockX() + 0.5D);
            updated.set(base + ".y", location.getBlockY());
            updated.set(base + ".z", location.getBlockZ() + 0.5D);
            updated.set(base + ".drop-height", Math.max(3.0D, dropHeight));
            saveAtomically(updated);
            return reload() && carePackages(arenaId).locations().containsKey(stableId);
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger().severe("Could not save Care Package location '" + stableId + "' for arena '" + arenaId + "': " + exception.getMessage());
            return false;
        }
    }

    public synchronized boolean removeCarePackage(String requestedArenaId, String name) {
        return removeFeatureLocation(requestedArenaId, "care-packages", name);
    }

    public synchronized boolean saveActivePlayersLeaderboard(String requestedArenaId, Location location) {
        String arenaId = normalizeArenaId(requestedArenaId);
        if (location == null || location.getWorld() == null || !arenas.containsKey(arenaId)) return false;
        try {
            YamlConfiguration updated = loadForWrite();
            String base = "arenas." + arenaId + ".leaderboards.activeplayers";
            updated.set(base + ".enabled", true);
            updated.set(base + ".location.world", location.getWorld().getName());
            updated.set(base + ".location.x", location.getX());
            updated.set(base + ".location.y", location.getY());
            updated.set(base + ".location.z", location.getZ());
            updated.set(base + ".location.yaw", location.getYaw());
            updated.set(base + ".location.pitch", location.getPitch());
            saveAtomically(updated);
            return reload();
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger().severe("Could not save active players leaderboard for arena '" + arenaId + "': " + exception.getMessage());
            return false;
        }
    }

    public synchronized boolean removeActivePlayersLeaderboard(String requestedArenaId) {
        String arenaId = normalizeArenaId(requestedArenaId);
        if (!arenas.containsKey(arenaId)) return false;
        try {
            YamlConfiguration updated = loadForWrite();
            String base = "arenas." + arenaId + ".leaderboards.activeplayers";
            if (!updated.contains(base)) return false;
            updated.set(base, null);
            saveAtomically(updated);
            return reload();
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger().severe("Could not remove active players leaderboard for arena '" + arenaId + "': " + exception.getMessage());
            return false;
        }
    }

    public Map<String, StoredLocation> activePlayersLeaderboards() {
        return displayLocations("activeplayers");
    }

    public synchronized boolean saveUnlockedFeaturesLeaderboard(String requestedArenaId, Location location) {
        return saveDisplayLocation(requestedArenaId, "unlockedfeatures", location);
    }

    public synchronized boolean removeUnlockedFeaturesLeaderboard(String requestedArenaId) {
        return removeDisplayLocation(requestedArenaId, "unlockedfeatures");
    }

    public Map<String, StoredLocation> unlockedFeaturesLeaderboards() {
        return displayLocations("unlockedfeatures");
    }

    private synchronized boolean saveDisplayLocation(String requestedArenaId, String displayId, Location location) {
        String arenaId = normalizeArenaId(requestedArenaId);
        if (location == null || location.getWorld() == null || !arenas.containsKey(arenaId)) return false;
        try {
            YamlConfiguration updated = loadForWrite();
            String base = "arenas." + arenaId + ".leaderboards." + displayId;
            updated.set(base + ".enabled", true);
            updated.set(base + ".location.world", location.getWorld().getName());
            updated.set(base + ".location.x", location.getX());
            updated.set(base + ".location.y", location.getY());
            updated.set(base + ".location.z", location.getZ());
            updated.set(base + ".location.yaw", location.getYaw());
            updated.set(base + ".location.pitch", location.getPitch());
            saveAtomically(updated);
            return reload();
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger().severe("Could not save " + displayId + " display for arena '" + arenaId + "': " + exception.getMessage());
            return false;
        }
    }

    private synchronized boolean removeDisplayLocation(String requestedArenaId, String displayId) {
        String arenaId = normalizeArenaId(requestedArenaId);
        if (!arenas.containsKey(arenaId)) return false;
        try {
            YamlConfiguration updated = loadForWrite();
            String base = "arenas." + arenaId + ".leaderboards." + displayId;
            if (!updated.contains(base)) return false;
            updated.set(base, null);
            saveAtomically(updated);
            return reload();
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger().severe("Could not remove " + displayId + " display for arena '" + arenaId + "': " + exception.getMessage());
            return false;
        }
    }

    private Map<String, StoredLocation> displayLocations(String displayId) {
        Map<String, StoredLocation> values = new LinkedHashMap<>();
        for (String arenaId : arenas.keySet()) {
            String base = "arenas." + arenaId + ".leaderboards." + displayId;
            if (!configuration.getBoolean(base + ".enabled", true)) continue;
            ConfigurationSection location = configuration.getConfigurationSection(base + ".location");
            if (!validCoordinates(location)) continue;
            String world = location.getString("world", arenas.get(arenaId).worldName());
            if (world == null || world.isEmpty()) continue;
            values.put(arenaId, new StoredLocation(world, location.getDouble("x"), location.getDouble("y"),
                    location.getDouble("z"), (float) location.getDouble("yaw"), (float) location.getDouble("pitch")));
        }
        return Collections.unmodifiableMap(values);
    }

    public Optional<ArenaDefinition> arena(String requestedArenaId) {
        return Optional.ofNullable(arenas.get(normalizeArenaId(requestedArenaId)));
    }

    public Map<String, ArenaDefinition> arenas() { return arenas; }

    public ArenaFeatureConfiguration.LaunchPads launchPads(String requestedArenaId) {
        return launchPads.getOrDefault(normalizeArenaId(requestedArenaId), disabledLaunchPads());
    }

    public ArenaFeatureConfiguration.CoinSpawns coinSpawns(String requestedArenaId) {
        return coinSpawns.getOrDefault(normalizeArenaId(requestedArenaId), disabledCoinSpawns());
    }

    public ArenaFeatureConfiguration.CarePackages carePackages(String requestedArenaId) {
        return carePackages.getOrDefault(normalizeArenaId(requestedArenaId), disabledCarePackages());
    }

    public Set<String> enabledArenaIds() {
        Set<String> ids = new LinkedHashSet<>();
        for (ArenaDefinition arena : arenas.values()) if (arena.enabled()) ids.add(arena.id());
        return Collections.unmodifiableSet(ids);
    }

    public Set<String> arenaIds() { return Collections.unmodifiableSet(new LinkedHashSet<>(arenas.keySet())); }

    public boolean containsArena(String requestedArenaId) { return arenas.containsKey(normalizeArenaId(requestedArenaId)); }

    public synchronized Map<String, StoredLocation> eventLocations(String requestedArenaId, String requestedEventId) {
        String arenaId = normalizeArenaId(requestedArenaId);
        String eventId = normalizeEventId(requestedEventId);
        if (!arenas.containsKey(arenaId) || eventId.isEmpty()) return Collections.emptyMap();
        String base = "arenas." + arenaId + ".events.";
        Map<String, StoredLocation> result = new LinkedHashMap<>();
        List<String> paths = eventId.indexOf('_') < 0
                ? Collections.singletonList(base + eventId + ".locations")
                : List.of(base + eventId + ".locations", base + eventId.replace('_', '-') + ".locations");
        for (String path : paths) {
            ConfigurationSection section = configuration.getConfigurationSection(path);
            if (section == null) continue;
            for (String rawId : section.getKeys(false)) {
                String id = normalizeStableId(rawId);
                ConfigurationSection value = section.getConfigurationSection(rawId);
                if (id.isEmpty() || !validCoordinates(value)) continue;
                String world = value.getString("world", arenas.get(arenaId).worldName());
                result.put(id, new StoredLocation(world, value.getDouble("x"), value.getDouble("y"), value.getDouble("z"),
                        (float) value.getDouble("yaw"), (float) value.getDouble("pitch")));
            }
        }
        return Collections.unmodifiableMap(result);
    }

    public record RagePitBlock(int x, int y, int z) {}
    public enum RagePitShape { CIRCLE, ELLIPSE, SQUARE, RECTANGLE, POLYGON }
    public record RagePitDefinition(boolean enabled, RagePitShape shape, String worldName, StoredLocation center,
                                    StoredLocation cornerOne, StoredLocation cornerTwo, int radiusX, int radiusZ, int minimumY,
                                    int maximumY, int thickness, org.bukkit.Material material, byte data,
                                    List<RagePitBlock> boundary, String error) {
        public boolean valid() { return error == null || error.isEmpty(); }
    }
    private record RagePitRead(RagePitDefinition definition, boolean migrated) {}

    public RagePitDefinition ragePit(String requestedArenaId) {
        return ragePits.get(normalizeArenaId(requestedArenaId));
    }

    public synchronized boolean saveRagePitCenter(String requestedArenaId, Location location) {
        String id = normalizeArenaId(requestedArenaId);
        ArenaDefinition arena = arenas.get(id);
        if (arena == null || location == null || location.getWorld() == null || !arena.worldName().equals(location.getWorld().getName())) return false;
        return updateRagePit(id, updated -> {
            String base = "arenas." + id + ".rage-pit";
            updated.set(base + ".enabled", true);
            if (!updated.isSet(base + ".shape")) updated.set(base + ".shape", "CIRCLE");
            updated.set(base + ".center.world", arena.worldName());
            updated.set(base + ".center.x", location.getX());
            updated.set(base + ".center.y", location.getY());
            updated.set(base + ".center.z", location.getZ());
            if (!updated.isSet(base + ".radius-x")) updated.set(base + ".radius-x", 25);
            if (!updated.isSet(base + ".radius-z")) updated.set(base + ".radius-z", 25);
            if (!updated.isSet(base + ".minimum-y")) updated.set(base + ".minimum-y", Math.max(0, location.getBlockY() - 27));
            if (!updated.isSet(base + ".maximum-y")) updated.set(base + ".maximum-y", Math.min(255, location.getBlockY() + 23));
            if (!updated.isSet(base + ".thickness")) updated.set(base + ".thickness", 1);
            if (!updated.isSet(base + ".material")) updated.set(base + ".material", "STAINED_GLASS");
            if (!updated.isSet(base + ".data")) updated.set(base + ".data", 14);
            updated.set("arenas." + id + ".events.rage-pit.blocks", null);
        });
    }

    public synchronized boolean saveRagePitShape(String requestedArenaId, String requestedShape) {
        String id = normalizeArenaId(requestedArenaId);
        RagePitShape shape;
        try { shape = RagePitShape.valueOf(requestedShape.toUpperCase(Locale.ENGLISH)); }
        catch (IllegalArgumentException exception) { return false; }
        if (!arenas.containsKey(id)) return false;
        return updateRagePit(id, updated -> {
            String base = "arenas." + id + ".rage-pit";
            updated.set(base + ".enabled", true);
            updated.set(base + ".shape", shape.name());
        });
    }

    public synchronized boolean saveRagePitCorner(String requestedArenaId, int corner, Location location) {
        String id = normalizeArenaId(requestedArenaId);
        ArenaDefinition arena = arenas.get(id);
        if (arena == null || location == null || location.getWorld() == null || !arena.worldName().equals(location.getWorld().getName()) || corner < 1 || corner > 2) return false;
        return updateRagePit(id, updated -> {
            String base = "arenas." + id + ".rage-pit.corner-" + corner;
            updated.set(base + ".world", arena.worldName());
            updated.set(base + ".x", location.getBlockX());
            updated.set(base + ".z", location.getBlockZ());
        });
    }

    public synchronized boolean saveRagePitRadius(String requestedArenaId, int radiusX, int radiusZ) {
        String id = normalizeArenaId(requestedArenaId);
        if (!arenas.containsKey(id) || radiusX < 2 || radiusZ < 2) return false;
        return updateRagePit(id, updated -> {
            String base = "arenas." + id + ".rage-pit";
            updated.set(base + ".radius-x", radiusX);
            updated.set(base + ".radius-z", radiusZ);
        });
    }

    public synchronized boolean saveRagePitHeight(String requestedArenaId, int minimumY, int maximumY) {
        String id = normalizeArenaId(requestedArenaId);
        if (!arenas.containsKey(id) || minimumY < 0 || maximumY > 255 || minimumY > maximumY) return false;
        return updateRagePit(id, updated -> {
            String base = "arenas." + id + ".rage-pit";
            updated.set(base + ".minimum-y", minimumY);
            updated.set(base + ".maximum-y", maximumY);
        });
    }

    private boolean updateRagePit(String id, java.util.function.Consumer<YamlConfiguration> change) {
        try {
            YamlConfiguration updated = loadForWrite();
            change.accept(updated);
            saveAtomically(updated);
            return reload() && ragePits.containsKey(id);
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger().severe("Could not save Rage Pit configuration for arena '" + id + "': " + exception.getMessage());
            return false;
        }
    }

    private RagePitRead readRagePit(YamlConfiguration loaded, ConfigurationSection arena, String id, String world) {
        String base = "arenas." + id + ".rage-pit";
        ConfigurationSection section = loaded.getConfigurationSection(base);
        boolean migrated = false;
        if (section == null) return new RagePitRead(invalidRagePit("missing YAML path " + base), migrated);
        if (!section.getBoolean("enabled", false)) return new RagePitRead(invalidRagePit("disabled YAML path " + base + ".enabled"), migrated);
        RagePitShape shape;
        try { shape = RagePitShape.valueOf(section.getString("shape", "").toUpperCase(Locale.ENGLISH)); }
        catch (IllegalArgumentException exception) { return new RagePitRead(invalidRagePit("invalid YAML path " + base + ".shape"), migrated); }
        if (id.equals("castle") && shape != RagePitShape.SQUARE) return new RagePitRead(invalidRagePit("arenas.castle.rage-pit.shape must be SQUARE"), migrated);
        ConfigurationSection center = section.getConfigurationSection("center");
        StoredLocation location = null;
        if (shape == RagePitShape.CIRCLE || shape == RagePitShape.ELLIPSE) {
            if (center == null) return new RagePitRead(invalidRagePit("missing YAML path " + base + ".center"), migrated);
            String centerWorld = center.getString("world");
            if (centerWorld == null) return new RagePitRead(invalidRagePit("missing YAML path " + base + ".center.world"), migrated);
            if (!world.equals(centerWorld)) return new RagePitRead(invalidRagePit("world mismatch at " + base + ".center.world"), migrated);
            location = new StoredLocation(centerWorld, center.getDouble("x"), center.getDouble("y"), center.getDouble("z"), 0.0F, 0.0F);
        }
        int rx = section.getInt("radius-x", 0), rz = section.getInt("radius-z", 0), minY = section.getInt("minimum-y", -1), maxY = section.getInt("maximum-y", -1), thickness = section.getInt("thickness", 1);
        List<Map<?, ?>> polygon = section.getMapList("polygon-points");
        if ((shape == RagePitShape.CIRCLE || shape == RagePitShape.ELLIPSE) && rx < 2) return new RagePitRead(invalidRagePit("invalid YAML path " + base + ".radius-x"), migrated);
        if ((shape == RagePitShape.CIRCLE || shape == RagePitShape.ELLIPSE) && rz < 2) return new RagePitRead(invalidRagePit("invalid YAML path " + base + ".radius-z"), migrated);
        if (shape == RagePitShape.CIRCLE && rx != rz) return new RagePitRead(invalidRagePit("CIRCLE requires equal radius-x and radius-z, found " + rx + " and " + rz), migrated);
        if (shape == RagePitShape.POLYGON && polygon.size() < 3) return new RagePitRead(invalidRagePit("invalid YAML path " + base + ".polygon-points; at least 3 ordered points are required"), migrated);
        if (minY < 0 || maxY > 255 || minY > maxY) return new RagePitRead(invalidRagePit("invalid YAML paths " + base + ".minimum-y/maximum-y"), migrated);
        if (thickness < 1 || (shape == RagePitShape.CIRCLE || shape == RagePitShape.ELLIPSE) && thickness > Math.min(rx, rz)) return new RagePitRead(invalidRagePit("invalid YAML path " + base + ".thickness"), migrated);
        org.bukkit.Material material = org.bukkit.Material.matchMaterial(section.getString("material", "STAINED_GLASS"));
        if (material == null || !material.isBlock()) return new RagePitRead(invalidRagePit("invalid YAML path " + base + ".material"), migrated);
        StoredLocation cornerOne = readRagePitCorner(section, base, "corner-1", world);
        StoredLocation cornerTwo = readRagePitCorner(section, base, "corner-2", world);
        if ((shape == RagePitShape.SQUARE || shape == RagePitShape.RECTANGLE) && cornerOne == null) return new RagePitRead(invalidRagePit("missing or invalid YAML path " + base + ".corner-1"), migrated);
        if ((shape == RagePitShape.SQUARE || shape == RagePitShape.RECTANGLE) && cornerTwo == null) return new RagePitRead(invalidRagePit("missing or invalid YAML path " + base + ".corner-2"), migrated);
        int sideX = cornerOne == null || cornerTwo == null ? 0 : Math.abs((int) cornerTwo.x() - (int) cornerOne.x());
        int sideZ = cornerOne == null || cornerTwo == null ? 0 : Math.abs((int) cornerTwo.z() - (int) cornerOne.z());
        if ((shape == RagePitShape.SQUARE || shape == RagePitShape.RECTANGLE) && (sideX < 2 || sideZ < 2)) return new RagePitRead(invalidRagePit(shape + " side lengths must both be at least 2, found x=" + sideX + " z=" + sideZ), migrated);
        if (shape == RagePitShape.SQUARE && sideX != sideZ) return new RagePitRead(invalidRagePit("SQUARE side lengths are unequal: x=" + sideX + " z=" + sideZ), migrated);
        if ((shape == RagePitShape.SQUARE || shape == RagePitShape.RECTANGLE) && thickness * 2 > Math.min(sideX + 1, sideZ + 1)) return new RagePitRead(invalidRagePit("thickness " + thickness + " would fill the " + sideX + " x " + sideZ + " interior"), migrated);
        List<RagePitBlock> boundary = switch (shape) {
            case CIRCLE, ELLIPSE -> generateBoundary(location, rx, rz, minY, maxY, thickness);
            case SQUARE, RECTANGLE -> generateRectangularBoundary(cornerOne, cornerTwo, minY, maxY, thickness);
            case POLYGON -> generatePolygonBoundary(polygon, minY, maxY, thickness);
        };
        if (boundary.isEmpty()) return new RagePitRead(invalidRagePit("invalid YAML path " + base + ".polygon-points"), migrated);
        return new RagePitRead(new RagePitDefinition(true, shape, world, location, cornerOne, cornerTwo, rx, rz, minY, maxY, thickness, material, (byte) section.getInt("data", 14), boundary, ""), migrated);
    }

    private RagePitDefinition invalidRagePit(String error) {
        return new RagePitDefinition(false, null, null, null, null, null, 0, 0, 0, 0, 0, org.bukkit.Material.STAINED_GLASS, (byte) 14, Collections.emptyList(), error);
    }

    private List<RagePitBlock> generateBoundary(StoredLocation center, int rx, int rz, int minY, int maxY, int thickness) {
        int centerX = (int) Math.floor(center.x()), centerZ = (int) Math.floor(center.z());
        int innerX = Math.max(0, rx - thickness), innerZ = Math.max(0, rz - thickness);
        List<RagePitBlock> result = new ArrayList<>();
        for (int x = -rx; x <= rx; x++) for (int z = -rz; z <= rz; z++) {
            double outer = x * x / (double) (rx * rx) + z * z / (double) (rz * rz);
            double inner = innerX == 0 || innerZ == 0 ? 2.0D : x * x / (double) (innerX * innerX) + z * z / (double) (innerZ * innerZ);
            if (outer > 1.08D || inner < 1.0D) continue;
            for (int y = minY; y <= maxY; y++) result.add(new RagePitBlock(centerX + x, y, centerZ + z));
        }
        return Collections.unmodifiableList(result);
    }

    private StoredLocation readRagePitCorner(ConfigurationSection section, String base, String path, String arenaWorld) {
        ConfigurationSection corner = section.getConfigurationSection(path);
        if (corner == null || !corner.contains("x") || !corner.contains("z")) return null;
        String world = corner.getString("world");
        if (world == null || !arenaWorld.equals(world)) return null;
        return new StoredLocation(world, corner.getDouble("x"), 0.0D, corner.getDouble("z"), 0.0F, 0.0F);
    }

    private List<RagePitBlock> generateRectangularBoundary(StoredLocation first, StoredLocation second, int minY, int maxY, int thickness) {
        int minX = (int) Math.min(first.x(), second.x()), maxX = (int) Math.max(first.x(), second.x());
        int minZ = (int) Math.min(first.z(), second.z()), maxZ = (int) Math.max(first.z(), second.z());
        Set<String> columns = new LinkedHashSet<>();
        for (int layer = 0; layer < thickness; layer++) {
            int lowX = minX + layer, highX = maxX - layer, lowZ = minZ + layer, highZ = maxZ - layer;
            if (lowX > highX || lowZ > highZ) break;
            for (int x = lowX; x <= highX; x++) {
                columns.add(x + ":" + lowZ);
                columns.add(x + ":" + highZ);
            }
            for (int z = lowZ; z <= highZ; z++) {
                columns.add(lowX + ":" + z);
                columns.add(highX + ":" + z);
            }
        }
        List<RagePitBlock> result = new ArrayList<>();
        for (String column : columns) {
            String[] values = column.split(":");
            int x = Integer.parseInt(values[0]), z = Integer.parseInt(values[1]);
            for (int y = minY; y <= maxY; y++) result.add(new RagePitBlock(x, y, z));
        }
        return Collections.unmodifiableList(result);
    }

    private List<RagePitBlock> generatePolygonBoundary(List<Map<?, ?>> configured, int minY, int maxY, int thickness) {
        List<int[]> points = new ArrayList<>();
        for (Map<?, ?> value : configured) {
            Object rawX = value.get("x"), rawZ = value.get("z");
            if (!(rawX instanceof Number) || !(rawZ instanceof Number)) return Collections.emptyList();
            points.add(new int[]{((Number) rawX).intValue(), ((Number) rawZ).intValue()});
        }
        Set<String> columns = new LinkedHashSet<>();
        int spread = Math.max(0, thickness - 1);
        for (int index = 0; index < points.size(); index++) {
            int[] from = points.get(index), to = points.get((index + 1) % points.size());
            int x = from[0], z = from[1], dx = Math.abs(to[0] - x), dz = Math.abs(to[1] - z), sx = x < to[0] ? 1 : -1, sz = z < to[1] ? 1 : -1, error = dx - dz;
            while (true) {
                for (int ox = -spread; ox <= spread; ox++) for (int oz = -spread; oz <= spread; oz++) columns.add((x + ox) + ":" + (z + oz));
                if (x == to[0] && z == to[1]) break;
                int doubled = error * 2;
                if (doubled > -dz) { error -= dz; x += sx; }
                if (doubled < dx) { error += dx; z += sz; }
            }
        }
        List<RagePitBlock> result = new ArrayList<>();
        for (String column : columns) {
            String[] values = column.split(":"); int x = Integer.parseInt(values[0]), z = Integer.parseInt(values[1]);
            for (int y = minY; y <= maxY; y++) result.add(new RagePitBlock(x, y, z));
        }
        return Collections.unmodifiableList(result);
    }

    public synchronized boolean saveEventLocation(String requestedArenaId, String requestedEventId, String requestedLocationId, Location location) {
        String arenaId = normalizeArenaId(requestedArenaId);
        String eventId = normalizeEventId(requestedEventId);
        String locationId = normalizeStableId(requestedLocationId);
        if (!arenas.containsKey(arenaId) || eventId.isEmpty() || locationId.isEmpty() || location == null || location.getWorld() == null) return false;
        try {
            YamlConfiguration updated = loadForWrite();
            String eventPath = "arenas." + arenaId + ".events." + eventId;
            if (!updated.isConfigurationSection(eventPath)) eventPath = "arenas." + arenaId + ".events." + eventId.replace('_', '-');
            String path = eventPath + ".locations." + locationId;
            updated.set(path + ".x", location.getBlockX());
            updated.set(path + ".y", location.getBlockY());
            updated.set(path + ".z", location.getBlockZ());
            updated.set(path + ".yaw", location.getYaw());
            updated.set(path + ".pitch", location.getPitch());
            saveAtomically(updated);
            return reload() && eventLocations(arenaId, eventId).containsKey(locationId);
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger().severe("Could not save event location for arena '" + arenaId + "': " + exception.getMessage());
            return false;
        }
    }

    public synchronized boolean removeEventLocation(String requestedArenaId, String requestedEventId, String requestedLocationId) {
        String arenaId = normalizeArenaId(requestedArenaId);
        String eventId = normalizeEventId(requestedEventId);
        String locationId = normalizeStableId(requestedLocationId);
        if (!arenas.containsKey(arenaId) || eventId.isEmpty() || locationId.isEmpty()) return false;
        String base = "arenas." + arenaId + ".events.";
        String path = base + eventId.replace('_', '-') + ".locations." + locationId;
        try {
            YamlConfiguration updated = loadForWrite();
            if (!updated.contains(path)) path = base + eventId + ".locations." + locationId;
            if (!updated.contains(path)) return false;
            updated.set(path, null);
            saveAtomically(updated);
            return reload() && !eventLocations(arenaId, eventId).containsKey(locationId);
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger().severe("Could not remove event location for arena '" + arenaId + "': " + exception.getMessage());
            return false;
        }
    }

    public String spawnSource(String requestedArenaId) {
        String id = normalizeArenaId(requestedArenaId);
        return spawnSources.getOrDefault(id, "WORLD_FALLBACK");
    }

    public Map<String, Object> rawSpawnValues(String requestedArenaId) {
        String id = normalizeArenaId(requestedArenaId);
        Map<String, Object> values = new LinkedHashMap<>();
        Object raw = configuration.get("arenas." + id + ".spawn");
        if (raw instanceof ConfigurationSection section) {
            for (String key : section.getKeys(false)) values.put(key, section.get(key));
        } else if (raw != null) {
            values.put("legacy", raw);
        }
        return values;
    }

    public File file() { return file; }

    public static String normalizeArenaId(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase(Locale.ENGLISH).replaceAll("\\s+", "_");
    }

    public static String normalizeStableId(String value) {
        if (value == null) return "";
        String normalized = value.trim().toLowerCase(Locale.ENGLISH);
        return normalized.matches("[a-z0-9_-]+") ? normalized : "";
    }

    private static String normalizeEventId(String value) {
        String normalized = normalizeStableId(value).replace('-', '_');
        return normalized.equals("giantcake") ? "giant_cake" : normalized;
    }

    private YamlConfiguration loadForWrite() throws IOException, InvalidConfigurationException {
        YamlConfiguration loaded = new YamlConfiguration();
        loaded.load(file);
        return loaded;
    }

    private boolean normalizeArenaKeys(YamlConfiguration yaml, ConfigurationSection root) throws InvalidConfigurationException {
        boolean changed = false;
        Set<String> normalizedIds = new LinkedHashSet<>();
        for (String rawId : new ArrayList<>(root.getKeys(false))) {
            String id = normalizeArenaId(rawId);
            if (!normalizedIds.add(id)) throw new InvalidConfigurationException("Duplicate normalized arena ID: " + id);
            if (rawId.equals(id)) continue;
            ConfigurationSection source = root.getConfigurationSection(rawId);
            if (source == null) throw new InvalidConfigurationException("Invalid arena section: arenas." + rawId);
            ConfigurationSection target = yaml.createSection("arenas." + id);
            copySection(source, target);
            yaml.set("arenas." + rawId, null);
            changed = true;
        }
        return changed;
    }

    private void copySection(ConfigurationSection source, ConfigurationSection target) {
        for (String key : source.getKeys(false)) {
            ConfigurationSection child = source.getConfigurationSection(key);
            if (child == null) target.set(key, source.get(key));
            else copySection(child, target.createSection(key));
        }
    }

    private String requiredString(ConfigurationSection section, String arenaId, String field) throws InvalidConfigurationException {
        String value = section.getString(field);
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidConfigurationException("Invalid arena '" + arenaId + "': missing arenas." + arenaId + "." + field);
        }
        return value.trim();
    }

    private LocationRead readSpawn(YamlConfiguration loaded, ConfigurationSection arena, String id) {
        ConfigurationSection nested = arena.getConfigurationSection("spawn");
        if (nested != null) {
            StoredLocation location = readRequiredLocation(nested, id, "spawn");
            return new LocationRead(location, location == null ? "WORLD_FALLBACK" : "YAML", false);
        }
        if (arena.isString("spawn")) {
            StoredLocation legacy = StoredLocation.parseLegacy(arena.getString("spawn"));
            if (legacy != null) {
                String base = "arenas." + id + ".spawn";
                loaded.set(base, null);
                writeLocation(loaded, base, legacy);
                return new LocationRead(legacy, "LEGACY_MIGRATED", true);
            }
            plugin.getLogger().severe("Invalid spawn for arena '" + id + "': malformed legacy arenas." + id + ".spawn");
            return new LocationRead(null, "WORLD_FALLBACK", false);
        }
        plugin.getLogger().warning("Arena '" + id + "' has no valid configured spawn. Using the world spawn temporarily.");
        return new LocationRead(null, "WORLD_FALLBACK", false);
    }

    private StoredLocation readRequiredLocation(ConfigurationSection section, String arenaId, String path) {
        String[] fields = {"world", "x", "y", "z", "yaw", "pitch"};
        for (String field : fields) {
            if (!section.contains(field)) {
                plugin.getLogger().severe("Invalid spawn for arena '" + arenaId + "': missing arenas." + arenaId + "." + path + "." + field);
                return null;
            }
            if (!field.equals("world") && !(section.get(field) instanceof Number)) {
                plugin.getLogger().severe("Invalid spawn for arena '" + arenaId + "': invalid arenas." + arenaId + "." + path + "." + field);
                return null;
            }
        }
        String world = section.getString("world");
        if (world == null || world.trim().isEmpty()) {
            plugin.getLogger().severe("Invalid spawn for arena '" + arenaId + "': missing arenas." + arenaId + "." + path + ".world");
            return null;
        }
        try {
            return new StoredLocation(
                    world.trim(),
                    section.getDouble("x"),
                    section.getDouble("y"),
                    section.getDouble("z"),
                    (float) section.getDouble("yaw"),
                    (float) section.getDouble("pitch")
            );
        } catch (RuntimeException exception) {
            plugin.getLogger().severe("Invalid spawn for arena '" + arenaId + "': " + exception.getMessage());
            return null;
        }
    }

    private StoredLocation readOptionalLocation(ConfigurationSection arena, String path, String id) {
        ConfigurationSection section = arena.getConfigurationSection(path);
        if (section == null) return null;
        String world = section.getString("world");
        if (world == null || !section.contains("x") || !section.contains("y") || !section.contains("z")) {
            plugin.getLogger().warning("Invalid region location for arena '" + id + "': arenas." + id + "." + path);
            return null;
        }
        return new StoredLocation(
                world,
                section.getDouble("x"),
                section.getDouble("y"),
                section.getDouble("z"),
                (float) section.getDouble("yaw", 0.0D),
                (float) section.getDouble("pitch", 0.0D)
        );
    }

    private ArenaFeatureConfiguration.LaunchPads readLaunchPads(ConfigurationSection arena, String arenaId, String arenaWorld) {
        ConfigurationSection root = arena.getConfigurationSection("launch-pads");
        ConfigurationSection defaultsSection = root == null ? null : root.getConfigurationSection("defaults");
        ArenaFeatureConfiguration.LaunchPadDefaults defaults = new ArenaFeatureConfiguration.LaunchPadDefaults(
                value(defaultsSection, "horizontal-power", 2.0D),
                value(defaultsSection, "vertical-power", 0.85D),
                longValue(defaultsSection, "cooldown-millis", 1000L),
                intValue(defaultsSection, "fall-protection-seconds", 5),
                stringValue(defaultsSection, "particle", "SLIME"),
                stringValue(defaultsSection, "sound", "IRONGOLEM_HIT")
        );
        Map<String, ArenaFeatureConfiguration.LaunchPad> values = new LinkedHashMap<>();
        ConfigurationSection locations = root == null ? null : root.getConfigurationSection("locations");
        if (locations != null) {
            for (String rawId : locations.getKeys(false)) {
                String id = normalizeStableId(rawId);
                ConfigurationSection section = locations.getConfigurationSection(rawId);
                if (id.isEmpty() || !rawId.equals(id) || section == null || values.containsKey(id) || !validCoordinates(section)) {
                    plugin.getLogger().warning("Invalid launch pad at arenas." + arenaId + ".launch-pads.locations." + rawId);
                    continue;
                }
                String world = stringValue(section, "world", arenaWorld);
                if (world.isEmpty()) {
                    plugin.getLogger().warning("Invalid launch pad world at arenas." + arenaId + ".launch-pads.locations." + rawId);
                    continue;
                }
                values.put(id, new ArenaFeatureConfiguration.LaunchPad(
                        id,
                        world,
                        section.getInt("x"),
                        section.getInt("y"),
                        section.getInt("z"),
                        value(section, "horizontal-power", defaults.horizontalPower()),
                        value(section, "vertical-power", defaults.verticalPower()),
                        longValue(section, "cooldown-millis", defaults.cooldownMillis()),
                        intValue(section, "fall-protection-seconds", defaults.fallProtectionSeconds()),
                        stringValue(section, "particle", defaults.particle()),
                        stringValue(section, "sound", defaults.sound())
                ));
            }
        }
        return new ArenaFeatureConfiguration.LaunchPads(root != null && root.getBoolean("enabled", true), defaults, values);
    }

    private ArenaFeatureConfiguration.CoinSpawns readCoinSpawns(ConfigurationSection arena, String arenaId, String arenaWorld) {
        ConfigurationSection root = arena.getConfigurationSection("coin-spawns");
        ConfigurationSection defaultsSection = root == null ? null : root.getConfigurationSection("defaults");
        ArenaFeatureConfiguration.CoinDefaults defaults = new ArenaFeatureConfiguration.CoinDefaults(
                intValue(defaultsSection, "reward", 5),
                intValue(defaultsSection, "respawn-seconds", 30),
                stringValue(defaultsSection, "display-name", "&6Gold Coin"),
                stringValue(defaultsSection, "pickup-message", "&6+%amount% Gold"),
                intValue(defaultsSection, "max-active", 40)
        );
        Map<String, ArenaFeatureConfiguration.CoinSpawn> values = new LinkedHashMap<>();
        ConfigurationSection locations = root == null ? null : root.getConfigurationSection("locations");
        int discovered = locations == null ? 0 : locations.getKeys(false).size();
        int maximum = Math.max(1, Math.min(2000, plugin.registry().settings().getInt("coin-spawns.maximum-configured-points", 2000)));
        Set<String> occupiedBlocks = new HashSet<>();
        if (locations != null) {
            for (String rawId : locations.getKeys(false)) {
                String id = normalizeStableId(rawId);
                ConfigurationSection section = locations.getConfigurationSection(rawId);
                String rejection = id.isEmpty() ? "invalid ID" : !rawId.equals(id) ? "ID is not normalized"
                        : section == null ? "location is not a section" : values.containsKey(id) ? "duplicate ID"
                        : !validCoordinates(section) ? "invalid coordinates" : values.size() >= maximum ? "configured point limit " + maximum + " reached" : "";
                if (!rejection.isEmpty()) {
                    plugin.getLogger().warning("Rejected coin spawn arena=" + arenaId + " id=" + rawId + " reason=" + rejection);
                    continue;
                }
                String world = stringValue(section, "world", arenaWorld);
                if (world.isEmpty()) {
                    plugin.getLogger().warning("Rejected coin spawn arena=" + arenaId + " id=" + rawId + " reason=missing world");
                    continue;
                }
                StoredLocation location = new StoredLocation(
                        world,
                        section.getDouble("x"),
                        section.getDouble("y"),
                        section.getDouble("z"),
                        (float) section.getDouble("yaw", 0.0D),
                        (float) section.getDouble("pitch", 0.0D)
                );
                String blockKey = world.toLowerCase(Locale.ENGLISH) + ":" + (int) Math.floor(location.x()) + ":"
                        + (int) Math.floor(location.y()) + ":" + (int) Math.floor(location.z());
                if (!occupiedBlocks.add(blockKey)) {
                    plugin.getLogger().warning("Rejected coin spawn arena=" + arenaId + " id=" + rawId + " reason=duplicate block location " + blockKey);
                    continue;
                }
                values.put(id, new ArenaFeatureConfiguration.CoinSpawn(
                        id,
                        location,
                        Math.max(0, intValue(section, "reward", defaults.reward())),
                        Math.max(0, intValue(section, "respawn-seconds", defaults.respawnSeconds()))
                ));
            }
        }
        if (plugin.registry().settings().getBoolean("coin-spawns.debug", false)) plugin.getLogger().info("Coin spawn load arena=" + arenaId
                + " discovered=" + discovered + " valid=" + values.size() + " rejected=" + (discovered - values.size()) + " maximum=" + maximum);
        return new ArenaFeatureConfiguration.CoinSpawns(root != null && root.getBoolean("enabled", true), defaults, values);
    }

    private ArenaFeatureConfiguration.CarePackages readCarePackages(ConfigurationSection arena, String arenaId, String arenaWorld) {
        ConfigurationSection root = arena.getConfigurationSection("care-packages");
        Map<String, ArenaFeatureConfiguration.CarePackageLocation> values = new LinkedHashMap<>();
        ConfigurationSection locations = root == null ? null : root.getConfigurationSection("locations");
        if (locations != null) {
            for (String rawId : locations.getKeys(false)) {
                String id = normalizeStableId(rawId);
                ConfigurationSection section = locations.getConfigurationSection(rawId);
                if (id.isEmpty() || !rawId.equals(id) || section == null || values.containsKey(id) || !validCoordinates(section)) {
                    plugin.getLogger().warning("Invalid Care Package location at arenas." + arenaId + ".care-packages.locations." + rawId);
                    continue;
                }
                String world = stringValue(section, "world", arenaWorld);
                if (!arenaWorld.equalsIgnoreCase(world)) {
                    plugin.getLogger().warning("Care Package location '" + rawId + "' belongs to a different arena world.");
                    continue;
                }
                StoredLocation location = new StoredLocation(world, section.getDouble("x"), section.getDouble("y"),
                        section.getDouble("z"), 0.0F, 0.0F);
                values.put(id, new ArenaFeatureConfiguration.CarePackageLocation(
                        id, location, Math.max(3.0D, section.getDouble("drop-height", 25.0D))));
            }
        }
        return new ArenaFeatureConfiguration.CarePackages(root != null && root.getBoolean("enabled", true), values);
    }

    private boolean migrateFeatureLayout(YamlConfiguration yaml, ConfigurationSection arena, String arenaId) {
        boolean changed = false;
        String launch = "arenas." + arenaId + ".launch-pads";
        ConfigurationSection launchSection = arena.getConfigurationSection("launch-pads");
        if (launchSection != null && launchSection.getConfigurationSection("defaults") == null) {
            yaml.set(launch + ".defaults.horizontal-power", launchSection.getDouble("horizontal-power", 2.0D));
            yaml.set(launch + ".defaults.vertical-power", launchSection.getDouble("vertical-power", 0.85D));
            yaml.set(launch + ".defaults.cooldown-millis", launchSection.getLong("cooldown-ms", 1000L));
            yaml.set(launch + ".defaults.fall-protection-seconds", launchSection.getInt("fall-protection-seconds", 5));
            yaml.set(launch + ".defaults.particle", launchSection.getString("particle-effect", "SLIME"));
            yaml.set(launch + ".defaults.sound", launchSection.getString("launch-sound", "IRONGOLEM_HIT"));
            yaml.set(launch + ".horizontal-power", null);
            yaml.set(launch + ".vertical-power", null);
            yaml.set(launch + ".cooldown-ms", null);
            yaml.set(launch + ".fall-protection-seconds", null);
            yaml.set(launch + ".particle-effect", null);
            yaml.set(launch + ".launch-sound", null);
            changed = true;
        }
        String oldCoins = "arenas." + arenaId + ".collectible-coins";
        ConfigurationSection oldCoinSection = arena.getConfigurationSection("collectible-coins");
        if (oldCoinSection != null) {
            String coins = "arenas." + arenaId + ".coin-spawns";
            yaml.set(coins + ".enabled", oldCoinSection.getBoolean("enabled", true));
            yaml.set(coins + ".defaults.reward", oldCoinSection.getInt("default-reward", 5));
            yaml.set(coins + ".defaults.respawn-seconds", oldCoinSection.getInt("default-respawn-seconds", 30));
            yaml.set(coins + ".defaults.display-name", oldCoinSection.getString("display-name", "&6Gold Coin"));
            yaml.set(coins + ".defaults.pickup-message", oldCoinSection.getString("pickup-message", "&6+%amount% Gold"));
            yaml.set(coins + ".defaults.max-active", oldCoinSection.getInt("max-active", 40));
            ConfigurationSection oldLocations = oldCoinSection.getConfigurationSection("locations");
            if (oldLocations != null) {
                for (String id : oldLocations.getKeys(false)) {
                    String oldBase = oldCoins + ".locations." + id;
                    String newBase = coins + ".locations." + id;
                    ConfigurationSection nested = oldLocations.getConfigurationSection(id + ".location");
                    if (nested != null) copyLocation(nested, yaml, newBase);
                    else {
                        ConfigurationSection flat = oldLocations.getConfigurationSection(id);
                        if (flat != null) copyLocation(flat, yaml, newBase);
                    }
                    yaml.set(newBase + ".reward", yaml.getInt(oldBase + ".reward", 5));
                    yaml.set(newBase + ".respawn-seconds", yaml.getInt(oldBase + ".respawn-seconds", 30));
                }
            }
            yaml.set(oldCoins, null);
            changed = true;
        }
        ConfigurationSection locations = yaml.getConfigurationSection(launch + ".locations");
        if (locations != null) {
            for (String id : locations.getKeys(false)) {
                String base = launch + ".locations." + id;
                if (yaml.contains(base + ".cooldown-ms")) {
                    yaml.set(base + ".cooldown-millis", yaml.getLong(base + ".cooldown-ms"));
                    yaml.set(base + ".cooldown-ms", null);
                    changed = true;
                }
                if (yaml.contains(base + ".particle-effect")) {
                    yaml.set(base + ".particle", yaml.getString(base + ".particle-effect"));
                    yaml.set(base + ".particle-effect", null);
                    changed = true;
                }
                if (yaml.contains(base + ".launch-sound")) {
                    yaml.set(base + ".sound", yaml.getString(base + ".launch-sound"));
                    yaml.set(base + ".launch-sound", null);
                    changed = true;
                }
            }
        }
        return changed;
    }

    private void copyLocation(ConfigurationSection source, YamlConfiguration target, String base) {
        for (String key : new String[]{"world", "x", "y", "z", "yaw", "pitch"}) if (source.contains(key)) target.set(base + "." + key, source.get(key));
    }

    private boolean removeFeatureLocation(String requestedArenaId, String feature, String name) {
        String arenaId = normalizeArenaId(requestedArenaId);
        String stableId = normalizeStableId(name);
        if (stableId.isEmpty() || !arenas.containsKey(arenaId)) return false;
        try {
            YamlConfiguration updated = loadForWrite();
            String path = "arenas." + arenaId + "." + feature + ".locations." + stableId;
            if (!updated.contains(path)) return false;
            updated.set(path, null);
            saveAtomically(updated);
            return reload();
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger().severe("Could not remove '" + stableId + "' from arenas.yml: " + exception.getMessage());
            return false;
        }
    }

    private boolean validCoordinates(ConfigurationSection section) {
        if (section == null) return false;
        for (String key : new String[]{"x", "y", "z"}) if (!(section.get(key) instanceof Number)) return false;
        return true;
    }

    private double value(ConfigurationSection section, String path, double fallback) {
        return section == null ? fallback : section.getDouble(path, fallback);
    }

    private long longValue(ConfigurationSection section, String path, long fallback) {
        return section == null ? fallback : section.getLong(path, fallback);
    }

    private int intValue(ConfigurationSection section, String path, int fallback) {
        return section == null ? fallback : section.getInt(path, fallback);
    }

    private String stringValue(ConfigurationSection section, String path, String fallback) {
        String value = section == null ? fallback : section.getString(path, fallback);
        return value == null ? fallback : value;
    }

    private ArenaFeatureConfiguration.LaunchPads disabledLaunchPads() {
        return new ArenaFeatureConfiguration.LaunchPads(false,
                new ArenaFeatureConfiguration.LaunchPadDefaults(2.0D, 0.85D, 1000L, 5, "SLIME", "IRONGOLEM_HIT"),
                Collections.emptyMap());
    }

    private ArenaFeatureConfiguration.CoinSpawns disabledCoinSpawns() {
        return new ArenaFeatureConfiguration.CoinSpawns(false,
                new ArenaFeatureConfiguration.CoinDefaults(5, 30, "&6Gold Coin", "&6+%amount% Gold", 40),
                Collections.emptyMap());
    }

    private ArenaFeatureConfiguration.CarePackages disabledCarePackages() {
        return new ArenaFeatureConfiguration.CarePackages(false, Collections.emptyMap());
    }

    private void writeLocation(YamlConfiguration yaml, String base, StoredLocation location) {
        yaml.set(base + ".world", location.worldName());
        yaml.set(base + ".x", location.x());
        yaml.set(base + ".y", location.y());
        yaml.set(base + ".z", location.z());
        yaml.set(base + ".yaw", location.yaw());
        yaml.set(base + ".pitch", location.pitch());
    }

    private void saveAtomically(YamlConfiguration yaml) throws IOException {
        Path target = file.toPath();
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), "arenas-", ".tmp");
        try {
            Files.writeString(temporary, yaml.saveToString(), StandardCharsets.UTF_8);
            YamlConfiguration validation = new YamlConfiguration();
            try {
                validation.load(temporary.toFile());
            } catch (InvalidConfigurationException exception) {
                throw new IOException("Temporary arenas.yml validation failed", exception);
            }
            if (validation.getConfigurationSection("arenas") == null) throw new IOException("Temporary arenas.yml has no arenas root");
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void logLoadedSpawn(ArenaDefinition arena) {
        StoredLocation spawn = arena.spawn();
        if (spawn == null) return;
        plugin.getLogger().info("Loaded arena '" + arena.id() + "' spawn: " + spawn.worldName() + " "
                + spawn.x() + ", " + spawn.y() + ", " + spawn.z() + " yaw=" + spawn.yaw() + " pitch=" + spawn.pitch());
    }

    private record LocationRead(StoredLocation location, String source, boolean migrated) {}
}
