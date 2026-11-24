package me.outerspace.thepit.managers;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.registry.definition.ArenaDefinition;
import me.outerspace.thepit.registry.definition.StoredLocation;
import me.outerspace.thepit.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public class ArenaManager {
    private final ThePitPlugin plugin;
    private final PitYamlManager yamlManager;
    private final Set<String> loggedFallbacks = new HashSet<>();
    private final Map<String, List<Consumer<World>>> worldLoadCallbacks = new HashMap<>();
    private String currentArena;
    private World currentWorld;
    private World retainedSpawnWorld;
    private int retainedSpawnChunkX;
    private int retainedSpawnChunkZ;
    private BukkitTask spawnChunkRetentionTask;

    public ArenaManager(ThePitPlugin plugin, PitYamlManager yamlManager) {
        this.plugin = plugin;
        this.yamlManager = yamlManager;
    }

    public Set<String> getArenaIds() { return yamlManager.enabledArenaIds(); }

    public Optional<ArenaDefinition> getArena(String id) { return yamlManager.arena(id); }

    public String getDisplayName(String id) {
        String normalized = normalize(id);
        return CC.color(getArena(normalized).map(ArenaDefinition::displayName).orElse(normalized));
    }

    public String getCurrentArena() { return currentArena; }

    public Location getSpawn(String id) { return resolveSpawn(id).location(); }

    public Location getCurrentSpawn() {
        if (currentArena == null) return null;
        ArenaDefinition definition = getArena(currentArena).orElse(null);
        StoredLocation spawn = definition == null ? null : definition.spawn();
        if (currentWorld != null && spawn != null) return new Location(currentWorld,spawn.x(),spawn.y(),spawn.z(),spawn.yaw(),spawn.pitch());
        return getSpawn(currentArena);
    }
    public World getCurrentWorld() { return currentWorld; }
    public String getWorldTemplate(String id) { return yamlManager.worldTemplate(id); }

    public SpawnResolution resolveSpawn(String requestedId) {
        String id = normalize(requestedId);
        ArenaDefinition definition = getArena(id).orElse(null);
        if (definition == null) return new SpawnResolution(null, "MISSING_ARENA", false);
        StoredLocation configured = definition.spawn();
        if (configured != null) {
            Location resolved = configured.resolve();
            return new SpawnResolution(resolved, yamlManager.spawnSource(id), false);
        }
        World world = Bukkit.getWorld(definition.worldName());
        if (world == null) return new SpawnResolution(null, "WORLD_FALLBACK", true);
        if (loggedFallbacks.add(id)) {
            plugin.getLogger().warning("Arena '" + id + "' has no valid configured spawn. Using the world spawn temporarily.");
        }
        return new SpawnResolution(world.getSpawnLocation(), "WORLD_FALLBACK", true);
    }

    public void changeArenaSafely(String requestedId, boolean teleportPlayers, Consumer<Boolean> callback) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> changeArenaSafely(requestedId, teleportPlayers, callback));
            return;
        }
        String id = normalize(requestedId);
        ArenaDefinition definition = getArena(id).orElse(null);
        if (definition == null) {
            plugin.getLogger().warning("Active arena '" + id + "' was not found in arenas.yml.");
            callback.accept(false);
            return;
        }
        ensureSpawnWorldLoaded(definition, world -> {
            if (world == null) {
                callback.accept(false);
                return;
            }
            applyArenaWorldRules(world);
            StoredLocation configuredSpawn = definition.spawn();
            SpawnResolution resolution = configuredSpawn == null ? new SpawnResolution(world.getSpawnLocation(), "WORLD_FALLBACK", true) : new SpawnResolution(new Location(world,configuredSpawn.x(),configuredSpawn.y(),configuredSpawn.z(),configuredSpawn.yaw(),configuredSpawn.pitch()), yamlManager.spawnSource(id), false);
            if (resolution.location() == null) {
                plugin.getLogger().severe("Arena '" + id + "' loaded but has no safe spawn; keeping '" + currentArena + "' active.");
                callback.accept(false);
                return;
            }
            if (!loadSpawnChunk(world, resolution.location())) {
                plugin.getLogger().severe("Arena '" + id + "' spawn chunk " + (resolution.location().getBlockX() >> 4) + ","
                        + (resolution.location().getBlockZ() >> 4) + " could not be loaded; keeping '" + currentArena + "' active.");
                callback.accept(false);
                return;
            }
            if (!usableSpawn(world, resolution.location())) {
                plugin.getLogger().severe("Arena '" + id + "' has an unsafe or invalid spawn at " + resolution.location().getBlockX() + ","
                        + resolution.location().getBlockY() + "," + resolution.location().getBlockZ() + "; keeping '" + currentArena + "' active.");
                callback.accept(false);
                return;
            }
            String previous = currentArena;
            World previousWorld = currentWorld;
            releaseActiveSpawnChunk();
            retainActiveSpawnChunk(world, resolution.location());
            cleanupOldArena(previousWorld);
            currentArena = id;
            currentWorld = world;
            refreshArenaDependents();
            initializeNewArena();
            if (plugin.getTablistManager() != null) {
                for (Player player : Bukkit.getOnlinePlayers()) plugin.getTablistManager().markRefresh(player, "ARENA_OR_WORLD_CHANGED");
            }
            if (teleportPlayers) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (plugin.getCombatLogManager() != null) plugin.getCombatLogManager().trustTeleport(player,5000L);
                    player.teleport(resolution.location());
                    player.setGameMode(GameMode.ADVENTURE);
                }
            }
            if (previousWorld != null && previousWorld != world) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    for (Player player : previousWorld.getPlayers()) { if (plugin.getCombatLogManager() != null) plugin.getCombatLogManager().trustTeleport(player,5000L); player.teleport(resolution.location()); }
                    if (!Bukkit.unloadWorld(previousWorld, false)) {
                        plugin.getLogger().warning("Old arena world '" + previousWorld.getName() + "' could not be unloaded.");
                    }
                }, 2L);
            }
            callback.accept(true);
        });
    }

    public void teleportToCurrentSpawn(Player player) {
        if (currentArena == null) {
            plugin.getLogger().warning("Active arena '' was not found in arenas.yml.");
            return;
        }
        teleportToArenaSpawn(player, currentArena);
    }

    public void teleportToArenaSpawn(Player player, String requestedId) {
        String id = normalize(requestedId);
        ArenaDefinition definition = getArena(id).orElse(null);
        if (definition == null) {
            plugin.getLogger().warning("Active arena '" + id + "' was not found in arenas.yml.");
            return;
        }
        ensureSpawnWorldLoaded(definition, world -> {
            SpawnResolution resolution = resolveSpawn(id);
            if (resolution.location() == null || !player.isOnline()) return;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) { if (plugin.getCombatLogManager() != null) plugin.getCombatLogManager().trustTeleport(player,3000L); player.teleport(resolution.location()); }
            });
        });
    }

    public boolean setArenaPoint(String arena, String region, String point, Location location) {
        boolean saved=yamlManager.saveRegionPoint(normalize(arena), region, point, StoredLocation.fromBukkit(location));
        if(saved)plugin.getSetupService().reloadFromStorage();
        return saved;
    }

    public boolean setSpawn(String arena, Location location) {
        String id = normalize(arena);
        boolean saved=yamlManager.saveSpawn(id, StoredLocation.fromBukkit(location));
        if(saved)plugin.getSetupService().reloadFromStorage();
        return saved;
    }

    public boolean setDeath(String arena, Location location) {
        return plugin.getSetupService().setLocation(normalize(arena), "DEATH", location);
    }

    public boolean reloadFromYaml() {
        boolean loaded = yamlManager.reload();
        if (!loaded) return false;
        loggedFallbacks.clear();
        if (currentArena != null && getArena(currentArena).isEmpty()) {
            plugin.getLogger().warning("Active arena '" + currentArena + "' was not found in arenas.yml.");
        }
        return true;
    }

    public void reloadOverrides() { reloadFromYaml(); }

    public boolean isInCurrentSpawnProtection(Location location) { return isInRegion(currentArena, "spawn-protection", location); }

    public boolean isInCurrentPvpArena(Location location) { return isInRegion(currentArena, "pvp-arena", location); }

    public boolean isInCurrentArena(Location location) { return isInCurrentSpawnProtection(location) || isInCurrentPvpArena(location); }

    public boolean isInRegion(String requestedArena, String region, Location location) {
        if (requestedArena == null || location == null || location.getWorld() == null) return false;
        ArenaDefinition definition = getArena(requestedArena).orElse(null);
        if (definition == null) return false;
        StoredLocation first = region.equals("spawn-protection") ? definition.spawnProtectionPos1() : definition.pvpPos1();
        StoredLocation second = region.equals("spawn-protection") ? definition.spawnProtectionPos2() : definition.pvpPos2();
        if (first == null || second == null) return false;
        World expected = normalize(requestedArena).equals(currentArena) ? currentWorld : Bukkit.getWorld(first.worldName());
        if (expected == null || expected != location.getWorld()) return false;
        double minX = Math.min(first.x(), second.x());
        double maxX = Math.max(first.x(), second.x());
        double minY = Math.min(first.y(), second.y());
        double maxY = Math.max(first.y(), second.y());
        double minZ = Math.min(first.z(), second.z());
        double maxZ = Math.max(first.z(), second.z());
        return location.getX() >= minX && location.getX() <= maxX
                && location.getY() >= minY && location.getY() <= maxY
                && location.getZ() >= minZ && location.getZ() <= maxZ;
    }

    public PitYamlManager yaml() { return yamlManager; }

    public boolean isWorldLoading(String worldName) {
        return worldName != null && worldLoadCallbacks.containsKey(worldName);
    }

    public boolean ensureActiveSpawnChunk() {
        if (!Bukkit.isPrimaryThread() || currentWorld == null) return false;
        Location spawn = getCurrentSpawn();
        return spawn != null && spawn.getWorld() == currentWorld && retainActiveSpawnChunk(currentWorld, spawn);
    }

    public void onWorldUnload(World world) {
        if (world != null && world == retainedSpawnWorld) releaseActiveSpawnChunk();
    }

    public void shutdown() {
        releaseActiveSpawnChunk();
        worldLoadCallbacks.clear();
    }

    public static String normalize(String id) { return PitYamlManager.normalizeArenaId(id); }

    private void ensureSpawnWorldLoaded(ArenaDefinition definition, Consumer<World> callback) {
        ensureWorldLoaded(yamlManager.worldTemplate(definition.id()), definition.worldType(), callback);
    }

    private void ensureWorldLoaded(String worldName, String type, Consumer<World> callback) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> ensureWorldLoaded(worldName, type, callback));
            return;
        }
        World existing = Bukkit.getWorld(worldName);
        if (existing != null) {
            callback.accept(existing);
            return;
        }
        List<Consumer<World>> callbacks = worldLoadCallbacks.computeIfAbsent(worldName, ignored -> new ArrayList<>());
        callbacks.add(callback);
        if (callbacks.size() > 1) return;
        if ("slime".equalsIgnoreCase(type) && plugin.registry().settings().getBoolean("settings.use-slimeworldmanager", true)) {
            Object result = tryLoadSlimeWorld(worldName);
            if (result == null) {
                List<Consumer<World>> failed = worldLoadCallbacks.remove(worldName);
                if (failed != null) for (Consumer<World> waiting : failed) waiting.accept(null);
                return;
            }
            if (result instanceof CompletionStage<?> stage) {
                stage.whenComplete((value, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (error != null) failWorldLoad(worldName,error.getMessage());
                    else if (value instanceof World loaded) completeWorldLoad(worldName,loaded);
                    else pollWorld(worldName,0);
                }));
                return;
            }
            if (result instanceof World loaded) {
                completeWorldLoad(worldName,loaded);
                return;
            }
        } else {
            Bukkit.createWorld(new WorldCreator(worldName));
        }
        pollWorld(worldName, 0);
    }

    private void pollWorld(String worldName, int attempts) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            completeWorldLoad(worldName,world);
            return;
        }
        if (attempts >= 200) {
            plugin.getLogger().severe("Arena world '" + worldName + "' did not finish loading.");
            List<Consumer<World>> callbacks = worldLoadCallbacks.remove(worldName);
            if (callbacks != null) for (Consumer<World> callback : callbacks) callback.accept(null);
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> pollWorld(worldName, attempts + 1), 1L);
    }

    private void completeWorldLoad(String requestedName,World world){applyArenaWorldRules(world);List<Consumer<World>> callbacks=worldLoadCallbacks.remove(requestedName);if(callbacks!=null)for(Consumer<World> callback:callbacks)callback.accept(world);}
    private void failWorldLoad(String requestedName,String reason){plugin.getLogger().severe("Arena world '"+requestedName+"' failed to load: "+reason);List<Consumer<World>> callbacks=worldLoadCallbacks.remove(requestedName);if(callbacks!=null)for(Consumer<World> callback:callbacks)callback.accept(null);}

    private Object tryLoadSlimeWorld(String worldName) {
        try {
            org.bukkit.plugin.Plugin swm = Bukkit.getPluginManager().getPlugin("SlimeWorldManager");
            if (swm == null) {
                plugin.getLogger().severe("SlimeWorldManager is required to load arena world '" + worldName + "'.");
                return null;
            }
            Method loadWorld = swm.getClass().getMethod("loadWorld", String.class, boolean.class);
            Object result = loadWorld.invoke(swm, worldName, true);
            if (result instanceof CompletionStage<?>) return result;
            if (result != null) {
                try {
                    Method generateWorld = swm.getClass().getMethod("generateWorld", result.getClass());
                    Object generated = generateWorld.invoke(swm, result);
                    return generated == null ? result : generated;
                } catch (NoSuchMethodException ignored) { }
            }
            if (result == null) plugin.getLogger().severe("SlimeWorldManager did not start loading arena world '" + worldName + "'.");
            return result;
        } catch (Throwable throwable) {
            plugin.getLogger().warning("Could not load slime world " + worldName + ": " + throwable.getMessage());
            return null;
        }
    }

    private void refreshArenaDependents() {
        if (plugin.getActionBarManager() != null) plugin.getActionBarManager().clearCombatStates();
        if (plugin.getSlimeLaunchPadManager() != null) plugin.getSlimeLaunchPadManager().reload();
        if (plugin.getCollectibleCoinManager() != null) plugin.getCollectibleCoinManager().onArenaChanged();
        if (plugin.getPerkListener() != null) plugin.getPerkListener().cleanupArena();
        if (plugin.getPerkManager() != null) plugin.getPerkManager().arenaCleanup();
        if (plugin.getKillstreakManager() != null) plugin.getKillstreakManager().arenaCleanup();
    }

    private void cleanupOldArena(World oldWorld) {
        if (plugin.getCombatLogManager() != null) plugin.getCombatLogManager().clearAllTrusted();
        if (plugin.getMinorEventManager() != null) plugin.getMinorEventManager().arenaCleanup();
        if (plugin.getActionBarManager() != null) plugin.getActionBarManager().clearCombatStates();
        if (plugin.getFishingManager() != null) plugin.getFishingManager().arenaCleanup();
        if (plugin.getShopManager() != null) plugin.getShopManager().arenaCleanup();
        if (plugin.getAchievementManager() != null) plugin.getAchievementManager().resetSessions();
        if (plugin.getContractManager() != null) plugin.getContractManager().clearSessions();
        if (plugin.getGameListener() != null) plugin.getGameListener().clearPlacedBlocks(oldWorld);
        if (plugin.getSlimeLaunchPadManager() != null) plugin.getSlimeLaunchPadManager().shutdown();
        if (plugin.getCollectibleCoinManager() != null) plugin.getCollectibleCoinManager().shutdown();
        if (plugin.getPerkListener() != null) plugin.getPerkListener().cleanupArena();
        if (plugin.getPerkManager() != null) plugin.getPerkManager().arenaCleanup();
        if (plugin.getKillstreakManager() != null) plugin.getKillstreakManager().arenaCleanup();
        if (plugin.getLeaderboardManager() != null) plugin.getLeaderboardManager().despawnAll();
        if (plugin.getNpcService() != null) plugin.getNpcService().shutdown();
    }

    private void initializeNewArena() {
        if (plugin.getSlimeLaunchPadManager() != null) plugin.getSlimeLaunchPadManager().reload();
        if (plugin.getCollectibleCoinManager() != null) plugin.getCollectibleCoinManager().start();
        if (plugin.getNpcService() != null) {
            plugin.getNpcService().load();
            plugin.getNpcService().spawnAll();
        }
        if (plugin.getLeaderboardManager() != null) {
            plugin.getLeaderboardManager().load();
            plugin.getLeaderboardManager().spawnAll();
        }
    }

    private boolean retainActiveSpawnChunk(World world, Location spawn) {
        if (!Bukkit.isPrimaryThread() || world == null || spawn == null || spawn.getWorld() != world) return false;
        int chunkX = spawn.getBlockX() >> 4;
        int chunkZ = spawn.getBlockZ() >> 4;
        if (!loadSpawnChunk(world, spawn)) return false;
        retainedSpawnWorld = world;
        retainedSpawnChunkX = chunkX;
        retainedSpawnChunkZ = chunkZ;
        if (spawnChunkRetentionTask == null) spawnChunkRetentionTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            World retained = retainedSpawnWorld;
            if (retained != null && Bukkit.getWorld(retained.getName()) == retained
                    && !retained.isChunkLoaded(retainedSpawnChunkX, retainedSpawnChunkZ)) retained.loadChunk(retainedSpawnChunkX, retainedSpawnChunkZ);
        }, 20L, 20L);
        return true;
    }

    private boolean loadSpawnChunk(World world, Location spawn) {
        if (!Bukkit.isPrimaryThread() || world == null || spawn == null || spawn.getWorld() != world) return false;
        int chunkX = spawn.getBlockX() >> 4;
        int chunkZ = spawn.getBlockZ() >> 4;
        try {
            if (!world.isChunkLoaded(chunkX, chunkZ)) world.loadChunk(chunkX, chunkZ);
            return world.isChunkLoaded(chunkX, chunkZ);
        } catch (Throwable failure) {
            plugin.getLogger().warning("Active arena spawn chunk load failed world=" + world.getName() + " chunk=" + chunkX + "," + chunkZ
                    + " reason=" + failure.getClass().getSimpleName() + ": " + failure.getMessage());
            return false;
        }
    }

    private boolean usableSpawn(World world, Location spawn) {
        if (spawn == null || spawn.getWorld() != world || !Double.isFinite(spawn.getX()) || !Double.isFinite(spawn.getY()) || !Double.isFinite(spawn.getZ())) return false;
        int y = spawn.getBlockY();
        return y >= 0 && y + 1 < world.getMaxHeight() && !spawn.getBlock().getType().isSolid()
                && !spawn.clone().add(0.0D, 1.0D, 0.0D).getBlock().getType().isSolid();
    }

    private void releaseActiveSpawnChunk() {
        if (spawnChunkRetentionTask != null) spawnChunkRetentionTask.cancel();
        spawnChunkRetentionTask = null;
        retainedSpawnWorld = null;
    }

    private void applyArenaWorldRules(World world) {
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setTime(6000L);
        world.setStorm(false);
        world.setThundering(false);
        try { world.setGameRuleValue("doMobSpawning", "false"); } catch (Throwable ignored) { }
        try { world.setGameRuleValue("mobGriefing", "false"); } catch (Throwable ignored) { }
        try { world.setGameRuleValue("doDaylightCycle", "false"); } catch (Throwable ignored) { }
        try { world.setGameRuleValue("doFireTick", "false"); } catch (Throwable ignored) { }
        try { world.setGameRuleValue("doMobLoot", "false"); } catch (Throwable ignored) { }
    }

    public record SpawnResolution(Location location, String source, boolean fallbackUsed) { }
}
