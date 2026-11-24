package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.hooks.AbelixHook;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VanishManager {
    private final JavaPlugin plugin;
    private final AbelixHook abelixHook;
    private final Set<UUID> vanished = ConcurrentHashMap.newKeySet();
    private final Set<UUID> apiRefreshInProgress = ConcurrentHashMap.newKeySet();
    private final Map<UUID, GameMode> spectatorOriginalModes = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastSneakMillis = new ConcurrentHashMap<>();

    public VanishManager(JavaPlugin plugin, AbelixHook abelixHook) {
        this.plugin = plugin;
        this.abelixHook = abelixHook;
    }

    public boolean toggle(Player player) {
        return setVanished(player, !isVanished(player));
    }

    public boolean setVanished(Player player, boolean value) {
        UUID uuid = player.getUniqueId();
        if (value) {
            vanished.add(uuid);
        } else {
            vanished.remove(uuid);
            restoreSpectatorMode(player);
        }
        applyVisibility(player);
        updateApiVanishStateAsync(player, value);
        return value;
    }

    public boolean isVanished(Player player) {
        if (player == null) return false;
        return vanished.contains(player.getUniqueId());
    }

    public void applyAll(Player player) {
        refreshApiVanishStateAsync(player);
        for (Player online : Bukkit.getOnlinePlayers()) {
            refreshApiVanishStateAsync(online);
            if (isVanished(online)) applyVisibility(online, player);
            if (isVanished(player)) applyVisibility(player, online);
        }
    }

    public void remove(Player player) {
        vanished.remove(player.getUniqueId());
        spectatorOriginalModes.remove(player.getUniqueId());
        lastSneakMillis.remove(player.getUniqueId());
    }

    public void handleSneak(Player player) {
        if (!isVanished(player)) return;
        long now = System.currentTimeMillis();
        long window = Math.max(100L, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong("vanish.double-shift-millis", 500L));
        long previous = lastSneakMillis.getOrDefault(player.getUniqueId(), 0L);
        lastSneakMillis.put(player.getUniqueId(), now);
        if (now - previous > window) return;

        if (player.getGameMode() == GameMode.SPECTATOR) {
            restoreSpectatorMode(player);
        } else {
            spectatorOriginalModes.put(player.getUniqueId(), player.getGameMode());
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    private void restoreSpectatorMode(Player player) {
        GameMode original = spectatorOriginalModes.remove(player.getUniqueId());
        if (original != null && player.getGameMode() == GameMode.SPECTATOR) player.setGameMode(original);
    }

    private void applyVisibility(Player vanishedPlayer) {
        for (Player viewer : Bukkit.getOnlinePlayers()) applyVisibility(vanishedPlayer, viewer);
    }

    private void applyVisibility(Player vanishedPlayer, Player viewer) {
        if (viewer.equals(vanishedPlayer)) return;
        if (vanished.contains(vanishedPlayer.getUniqueId()) && !viewer.hasPermission("fakevival.command.vanish.see")) {
            viewer.hidePlayer(plugin, vanishedPlayer);
        } else {
            viewer.showPlayer(plugin, vanishedPlayer);
        }
    }

    private void refreshApiVanishStateAsync(Player player) {
        if (player == null || !abelixHook.isHooked()) return;
        UUID uuid = player.getUniqueId();
        if (!apiRefreshInProgress.add(uuid)) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Boolean apiValue;
            try {
                apiValue = abelixHook.isVanished(player);
            } finally {
                apiRefreshInProgress.remove(uuid);
            }
            if (apiValue == null) return;
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player online = Bukkit.getPlayer(uuid);
                if (online == null) return;
                if (apiValue) vanished.add(uuid);
                else vanished.remove(uuid);
                applyVisibility(online);
            });
        });
    }

    private void updateApiVanishStateAsync(Player player, boolean value) {
        if (player == null || !abelixHook.isHooked()) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> abelixHook.setVanished(player, value));
    }
}
