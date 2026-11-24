package me.outerspace.thepit.managers;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class CollectibleCoinManager implements Listener {
    private static final String METADATA_KEY = "buildffa_collectible_coin";
    private final ThePitPlugin plugin;
    private final Map<String, CoinSpawnPoint> spawnPoints = new LinkedHashMap<>();
    private final Map<String, Item> activeCoins = new HashMap<>();
    private final Map<UUID, String> entityToSpawn = new HashMap<>();
    private final Map<String, Long> respawnAt = new HashMap<>();
    private final Deque<String> spawnQueue = new ArrayDeque<>();
    private final Set<String> queuedSpawns = new HashSet<>();
    private final Set<String> goldSpawnCollectionProcessing = ConcurrentHashMap.newKeySet();
    private BukkitTask task;
    private ArenaFeatureConfiguration.CoinSpawns configuration;
    private String activeArena;
    private long lastDebugAt;

    public CollectibleCoinManager(ThePitPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (task == null) task = Bukkit.getScheduler().runTaskTimer(plugin, new CoinTickTask(), 10L, 2L);
        reload();
    }

    public void reload() {
        removeManagedCoins();
        spawnPoints.clear();
        respawnAt.clear();
        spawnQueue.clear();
        queuedSpawns.clear();
        goldSpawnCollectionProcessing.clear();
        activeArena = plugin.getArenaManager().getCurrentArena();
        configuration = plugin.getYamlManager().coinSpawns(activeArena);
        if (activeArena == null || !configuration.enabled()) return;
        for (ArenaFeatureConfiguration.CoinSpawn spawn : configuration.locations().values()) {
            spawnPoints.put(spawn.id(), new CoinSpawnPoint(spawn.id(), spawn.location(), spawn.reward(), spawn.respawnSeconds()));
        }
        resetSpawnQueue();
    }

    public void onArenaChanged() {
        reload();
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        removeManagedCoins();
        spawnPoints.clear();
        respawnAt.clear();
        spawnQueue.clear();
        queuedSpawns.clear();
        activeArena = null;
        goldSpawnCollectionProcessing.clear();
    }

    public void respawnAll() {
        removeManagedCoins();
        respawnAt.clear();
        resetSpawnQueue();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(PlayerPickupItemEvent event) {
        if (isManaged(event.getItem())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMerge(ItemMergeEvent event) {
        if (isManaged(event.getEntity()) || isManaged(event.getTarget())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDespawn(ItemDespawnEvent event) {
        if (isManaged(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (isManaged(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        for (Map.Entry<String, Item> entry : new ArrayList<>(activeCoins.entrySet())) {
            Item item = entry.getValue();
            if (item == null || item.getWorld() != event.getWorld()) continue;
            item.remove();
            forgetCoin(entry.getKey(), item);
            respawnAt.put(entry.getKey(), 0L);
            enqueue(entry.getKey());
        }
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        if (spawnPoints.values().stream().anyMatch(point -> point.getStoredLocation().worldName().equals(event.getWorld().getName()))) {
            for (CoinSpawnPoint point : spawnPoints.values()) if (point.getStoredLocation().worldName().equals(event.getWorld().getName())) enqueue(point.getId());
        }
    }

    public String addSpawn(Player player, String requestedName, int reward, int respawnSeconds) {
        String arena = activeArenaAt(player.getLocation());
        if (arena == null) return "§cYou must be in the active arena world.";
        String id = PitYamlManager.normalizeStableId(requestedName);
        if (id.isEmpty()) return "§cNames may contain only a-z, 0-9, underscore, and hyphen.";
        if (spawnPoints.containsKey(id)) return "§cA coin spawn named §e" + id + " §calready exists.";
        if (spawnPoints.size() >= maximumConfigured()) return "§cThis arena has reached the configured limit of §e" + maximumConfigured() + " §ccoin spawns.";
        for (CoinSpawnPoint point : spawnPoints.values()) {
            Location existing = point.getLocation();
            if (existing != null && existing.getWorld() == player.getWorld() && existing.getBlockX() == player.getLocation().getBlockX()
                    && existing.getBlockY() == player.getLocation().getBlockY() && existing.getBlockZ() == player.getLocation().getBlockZ())
                return "§cA coin spawn already exists at this block: §e" + point.getId() + "§c.";
        }
        if (!plugin.getYamlManager().saveCoinSpawn(arena, id, player.getLocation(), reward, respawnSeconds)) return unavailable();
        reload();
        return "§aCoin spawn §f" + id + " §awas added to arena §f" + arena + "§a.";
    }

    public String removeSpawn(Player player, String requestedName) {
        String arena = activeArenaAt(player.getLocation());
        if (arena == null) return "§cYou must be in the active arena world.";
        String id = PitYamlManager.normalizeStableId(requestedName);
        if (id.isEmpty()) return "§cNames may contain only a-z, 0-9, underscore, and hyphen.";
        if (!spawnPoints.containsKey(id)) return "§cNo coin spawn named §e" + id + " §cexists in the active arena.";
        Item item = activeCoins.get(id);
        if (!plugin.getYamlManager().removeCoinSpawn(arena, id)) return unavailable();
        if (item != null) {
            entityToSpawn.remove(item.getUniqueId());
            item.remove();
        }
        reload();
        return "§aCoin spawn §f" + id + " §awas removed from arena §f" + arena + "§a.";
    }

    public List<String> listSpawns(String requestedArena, int requestedPage) {
        if (activeArena == null) return Collections.singletonList("§cNo active arena.");
        String arena = requestedArena == null || requestedArena.trim().isEmpty() ? activeArena : ArenaManager.normalize(requestedArena);
        if (!activeArena.equals(arena)) return Collections.singletonList("§cCoin spawn runtime details are available only for the active arena §e" + activeArena + "§c.");
        int pageSize = 10;
        int pages = Math.max(1, (spawnPoints.size() + pageSize - 1) / pageSize);
        int page = Math.max(1, Math.min(pages, requestedPage));
        List<String> out = new ArrayList<>();
        out.add("§6Coin spawns for §f" + activeArena + "§6: §e" + spawnPoints.size() + " §7(active: " + activeCoins.size() + ", page " + page + "/" + pages + ")");
        List<CoinSpawnPoint> points = new ArrayList<>(spawnPoints.values());
        int start = (page - 1) * pageSize;
        for (int index = start; index < Math.min(points.size(), start + pageSize); index++) {
            CoinSpawnPoint point = points.get(index);
            me.outerspace.thepit.registry.definition.StoredLocation location = point.getStoredLocation();
            out.add("§7" + point.getId() + ": §f" + location.worldName() + "," + location.x() + "," + location.y() + "," + location.z()
                    + " §7reward=§e" + point.getReward() + " §7respawn=§e" + point.getRespawnSeconds() + "s");
        }
        return out;
    }

    public String reloadActive(String requestedArena) {
        String arena = requestedArena == null || requestedArena.trim().isEmpty() ? activeArena : ArenaManager.normalize(requestedArena);
        if (activeArena == null || !activeArena.equals(arena)) return "§cOnly the active arena §e" + activeArena + " §ccan be reloaded here.";
        if (!plugin.getYamlManager().reload()) return "§cCould not reload arenas.yml. Existing coin spawns remain active.";
        reload();
        return "§aReloaded §e" + spawnPoints.size() + " §acoin spawns for arena §f" + activeArena + "§a.";
    }

    public List<String> locationIds() {
        return new ArrayList<>(spawnPoints.keySet());
    }

    public int getDefaultReward() {
        return configuration == null ? 2 : Math.max(0, configuration.defaults().reward());
    }

    public int getDefaultRespawnSeconds() {
        return configuration == null ? 30 : Math.max(0, configuration.defaults().respawnSeconds());
    }

    private void tick() {
        if (configuration == null || !configuration.enabled() || activeArena == null) return;
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Item> entry : new ArrayList<>(activeCoins.entrySet())) {
            CoinSpawnPoint point = spawnPoints.get(entry.getKey());
            Item item = entry.getValue();
            Location home = point == null ? null : point.getLocation();
            if (point == null || home == null || item == null || item.isDead() || !item.isValid()) {
                forgetCoin(entry.getKey(), item);
                if (point != null) {
                    respawnAt.put(entry.getKey(), now + point.getRespawnSeconds() * 1000L);
                    enqueue(entry.getKey());
                }
                continue;
            }
            if (item.getWorld() != home.getWorld() || item.getLocation().distanceSquared(home) > 0.01D) item.teleport(home);
            item.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
            item.setPickupDelay(Integer.MAX_VALUE);
            item.setTicksLived(1);
            Player collector = nearbyCollector(home);
            if (collector != null) collect(entry.getKey(), collector);
        }
        int spawned = 0;
        int skipped = 0;
        int attempts = Math.min(spawnBatchSize(), spawnQueue.size());
        for (int index = 0; index < attempts; index++) {
            if (activeCoins.size() >= activeCap()) break;
            String id = spawnQueue.pollFirst();
            if (id == null) break;
            queuedSpawns.remove(id);
            if (!spawnPoints.containsKey(id) || activeCoins.containsKey(id)) continue;
            if (respawnAt.getOrDefault(id, 0L) > now) {
                enqueue(id);
                skipped++;
                continue;
            }
            if (spawnCoin(id)) {
                respawnAt.remove(id);
                spawned++;
            } else {
                enqueue(id);
                skipped++;
            }
        }
        debugBatch(now, spawned, skipped);
    }

    private Player nearbyCollector(Location location) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld() != location.getWorld() || player.isDead() || player.getHealth() <= 0.0D
                    || player.getGameMode() == GameMode.SPECTATOR || player.hasMetadata("NPC")) continue;
            if (player.getLocation().distanceSquared(location) <= 1.44D) return player;
        }
        return null;
    }

    private void collect(String id, Player player) {
        if (id == null || player == null || !goldSpawnCollectionProcessing.add(id)) return;
        CoinSpawnPoint point = spawnPoints.get(id);
        Item item = activeCoins.get(id);
        if (point == null || item == null || !player.isOnline() || !plugin.getStatsManager().isProfileLoaded(player.getUniqueId())) {
            goldSpawnCollectionProcessing.remove(id);
            return;
        }
        int originalBalance = plugin.getPitGoldService().balance(player.getUniqueId());
        try {
            activeCoins.remove(id);
            entityToSpawn.remove(item.getUniqueId());
            item.remove();
            int baseReward = plugin.getPerkManager() == null ? point.getReward() : plugin.getPerkManager().collectibleCoinReward(player, point.getReward());
            int awarded = plugin.getPitGoldService().award(player, baseReward, ThePitPlugin.GoldSource.COLLECTIBLE);
            if (awarded <= 0) {
                plugin.getPitGoldService().set(player, originalBalance);
                respawnAt.put(id, System.currentTimeMillis() + 1000L);
                enqueue(id);
                return;
            }
            if (plugin.getContractManager() != null) plugin.getContractManager().onGroundIngot(player);
            plugin.getStatsManager().addLongStat(player.getUniqueId(), "ground_ingots_collected", 1L);
            if (plugin.getAchievementManager() != null) plugin.getAchievementManager().publish(
                    new AchievementManager.ProgressEvent(player.getUniqueId(), AchievementManager.EventType.EVENT_SCORE_CHANGED,
                            plugin.getStatsManager().getLongStat(player.getUniqueId(), "ground_ingots_collected"),
                            java.util.Map.of("id", "ground_ingots_collected"), false));
            plugin.getStatsManager().addXp(player, Math.max(0, plugin.registry().settings().getInt("levels.rewards.coin-pickup-xp", 1)), StatsManager.XpSource.EVENT);
            plugin.getBuildScoreboardManager().update(player);
            player.sendMessage(CC.color("&6+") + plugin.getPitGoldService().formatPitGold(awarded));
            playPickupEffects(point.getLocation());
            respawnAt.put(id, System.currentTimeMillis() + point.getRespawnSeconds() * 1000L);
            enqueue(id);
        } catch (Throwable throwable) {
            plugin.getPitGoldService().set(player, originalBalance);
            respawnAt.put(id, System.currentTimeMillis() + 1000L);
            enqueue(id);
            plugin.getLogger().severe("Gold Spawn collection failed: spawn=" + id + ", player=" + player.getUniqueId() + ", reason=" + throwable.getMessage());
        } finally {
            goldSpawnCollectionProcessing.remove(id);
        }
    }

    public List<String> diagnose() {
        List<String> lines = new ArrayList<>();
        lines.add("§6Gold Spawn diagnostics: §farena=" + activeArena + " §fconfigured=" + spawnPoints.size()
                + " §factive=" + activeCoins.size() + "/" + activeCap() + " §fqueue=" + spawnQueue.size()
                + " §frespawning=" + respawnAt.size() + " §fbatch=" + spawnBatchSize());
        return lines;
    }

    private boolean spawnCoin(String id) {
        if (configuration == null || !configuration.enabled() || activeCoins.containsKey(id)
                || activeCoins.size() >= activeCap()) return false;
        CoinSpawnPoint point = spawnPoints.get(id);
        Location location = point == null ? null : point.getLocation();
        if (location == null || !location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) return false;
        Item item = location.getWorld().dropItem(location, new ItemStack(Material.GOLD_INGOT, 1));
        item.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, id));
        item.setPickupDelay(Integer.MAX_VALUE);
        item.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
        item.setCustomName(CC.color(configuration.defaults().displayName()));
        item.setCustomNameVisible(true);
        activeCoins.put(id, item);
        entityToSpawn.put(item.getUniqueId(), id);
        return true;
    }

    private void resetSpawnQueue() {
        spawnQueue.clear();
        queuedSpawns.clear();
        List<String> shuffled = new ArrayList<>(spawnPoints.keySet());
        Collections.shuffle(shuffled);
        for (String id : shuffled) enqueue(id);
    }

    private void enqueue(String id) {
        if (id != null && spawnPoints.containsKey(id) && !activeCoins.containsKey(id) && queuedSpawns.add(id)) spawnQueue.addLast(id);
    }

    private int maximumConfigured() {
        return Math.max(1, Math.min(2000, plugin.registry().settings().getInt("coin-spawns.maximum-configured-points", 2000)));
    }

    private int activeCap() {
        return Math.max(0, Math.min(maximumConfigured(), plugin.registry().settings().getInt("coin-spawns.maximum-active-coins", 250)));
    }

    private int spawnBatchSize() {
        return Math.max(1, Math.min(100, plugin.registry().settings().getInt("coin-spawns.spawn-batch-size", 20)));
    }

    private void debugBatch(long now, int spawned, int skipped) {
        if (!plugin.registry().settings().getBoolean("coin-spawns.debug", false) || now - lastDebugAt < 5000L) return;
        lastDebugAt = now;
        plugin.getLogger().info("Coin spawn cycle arena=" + activeArena + " configured=" + spawnPoints.size()
                + " active=" + activeCoins.size() + " cap=" + activeCap() + " queue=" + spawnQueue.size()
                + " spawned=" + spawned + " skipped=" + skipped + " batch=" + spawnBatchSize());
    }

    private void removeManagedCoins() {
        for (Item item : new ArrayList<>(activeCoins.values())) if (item != null && !item.isDead()) item.remove();
        activeCoins.clear();
        entityToSpawn.clear();
    }

    private void forgetCoin(String id, Item item) {
        activeCoins.remove(id);
        if (item != null) entityToSpawn.remove(item.getUniqueId());
    }

    private boolean isManaged(Entity entity) {
        return entity instanceof Item && entityToSpawn.containsKey(entity.getUniqueId()) && entity.hasMetadata(METADATA_KEY);
    }

    private String activeArenaAt(Location location) {
        String arena = plugin.getArenaManager().getCurrentArena();
        if (arena == null || location.getWorld() == null) return null;
        String world = plugin.getArenaManager().getArena(arena).map(value -> value.worldName()).orElse("");
        return location.getWorld().getName().equals(world) ? arena : null;
    }

    private String unavailable() {
        return "§cCould not save arenas.yml. Nothing was changed.";
    }

    private void playPickupEffects(Location location) {
        if (location == null || location.getWorld() == null) return;
        try {
            location.getWorld().playSound(location, Sound.ORB_PICKUP, 1.0F, 1.25F);
        } catch (Throwable ignored) {
        }
        try {
            location.getWorld().spigot().playEffect(location, Effect.COLOURED_DUST, 0, 0, 1.0F, 0.75F, 0.0F, 1.0F, 12, 32);
        } catch (Throwable ignored) {
            try {
                location.getWorld().playEffect(location, Effect.POTION_BREAK, 0);
            } catch (Throwable ignoredAgain) {
            }
        }
    }

    private final class CoinTickTask implements Runnable {
        @Override
        public void run() {
            tick();
        }
    }
}
