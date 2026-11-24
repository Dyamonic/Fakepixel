package me.outerspace.thepit.managers;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.model.NetworkProfileSnapshot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class NetworkProfileCache implements Listener {
    private final ThePitPlugin plugin;
    private final Map<UUID, CacheEntry> profiles = new java.util.concurrent.ConcurrentHashMap<>();
    private final long maxAgeMillis;

    public NetworkProfileCache(ThePitPlugin plugin) {
        this.plugin = plugin;
        this.maxAgeMillis = Math.max(1000L, plugin.registry().tablist().updateTicks() * 25L);
    }

    public NetworkProfileSnapshot get(Player player) {
        if (player == null) return empty();
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        CacheEntry cached = profiles.get(uuid);
        if (cached != null && now - cached.refreshedAt < maxAgeMillis) return cached.snapshot;
        return refresh(player, now);
    }

    public Map<UUID, NetworkProfileSnapshot> snapshots(Collection<? extends Player> players) {
        Map<UUID, NetworkProfileSnapshot> result = new LinkedHashMap<>();
        for (Player player : players) result.put(player.getUniqueId(), get(player));
        return result;
    }

    public NetworkProfileSnapshot refresh(Player player) {
        return player == null ? empty() : refresh(player, System.currentTimeMillis());
    }

    public void invalidate(Player player) {
        if (player != null) profiles.remove(player.getUniqueId());
    }

    public Integer cachedGold(UUID uuid) {
        CacheEntry entry = uuid == null ? null : profiles.get(uuid);
        return entry == null ? null : entry.snapshot.getGold();
    }

    public NetworkProfileSnapshot cached(UUID uuid) {
        CacheEntry entry = uuid == null ? null : profiles.get(uuid);
        return entry == null ? null : entry.snapshot;
    }

    public boolean isCached(UUID uuid) {
        return uuid != null && profiles.containsKey(uuid);
    }

    public void adjustGold(Player player, int delta) {
        if (player == null) return;
        profiles.computeIfPresent(player.getUniqueId(), (uuid, entry) -> {
            NetworkProfileSnapshot old = entry.snapshot;
            int gold = Math.max(0, old.getGold() + delta);
            return new CacheEntry(new NetworkProfileSnapshot(old.getPrefix(), old.getPermissionPrefix(), old.getRankColor(), old.getGroup(), old.getPriority(), gold), System.currentTimeMillis());
        });
    }

    public void clear() {
        profiles.clear();
    }

    private NetworkProfileSnapshot refresh(Player player, long now) {
        CacheEntry previous = profiles.get(player.getUniqueId());
        NetworkProfileSnapshot snapshot = plugin.resolveNetworkProfile(player);
        profiles.put(player.getUniqueId(), new CacheEntry(snapshot, now));
        if (previous != null && (!previous.snapshot.getPrefix().equals(snapshot.getPrefix())
                || !previous.snapshot.getRankColor().equals(snapshot.getRankColor())) && plugin.getTablistManager() != null) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) plugin.getTablistManager().markRefresh(player, "ABELIX_PREFIX_CHANGED");
            });
        }
        return snapshot;
    }

    private NetworkProfileSnapshot empty() {
        return new NetworkProfileSnapshot("", "", "", "default", -1, 0);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        refresh(event.getPlayer());
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                refresh(event.getPlayer());
                if (plugin.getTablistManager() != null) plugin.getTablistManager().markRefresh(event.getPlayer(), "PROFILE_READY");
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        profiles.remove(event.getPlayer().getUniqueId());
    }

    private static final class CacheEntry {
        private final NetworkProfileSnapshot snapshot;
        private final long refreshedAt;

        private CacheEntry(NetworkProfileSnapshot snapshot, long refreshedAt) {
            this.snapshot = snapshot;
            this.refreshedAt = refreshedAt;
        }
    }
}
