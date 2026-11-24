package club.abelix.games.fakevival.listeners;

import club.abelix.games.fakevival.managers.NpcManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NpcVisibilityListener implements Listener {
    private final JavaPlugin plugin;
    private final NpcManager npcManager;
    private final Map<UUID, BukkitTask> pendingSetup = new ConcurrentHashMap<>();

    public NpcVisibilityListener(JavaPlugin plugin, NpcManager npcManager) {
        this.plugin = plugin;
        this.npcManager = npcManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID viewerId = event.getPlayer().getUniqueId();
        BukkitTask previous = pendingSetup.remove(viewerId);
        if (previous != null) previous.cancel();
        npcManager.forgetViewer(event.getPlayer());
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingSetup.remove(viewerId);
            if (event.getPlayer().isOnline()) npcManager.showNpcs(event.getPlayer());
        }, 8L);
        pendingSetup.put(viewerId, task);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        BukkitTask task = pendingSetup.remove(event.getPlayer().getUniqueId());
        if (task != null) task.cancel();
        npcManager.forgetViewer(event.getPlayer());
    }
}
