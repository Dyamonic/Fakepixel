package club.abelix.games.fakevival.listeners;

import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.managers.BoardManager;
import club.abelix.games.fakevival.managers.SpawnManager;
import club.abelix.games.fakevival.managers.TablistManager;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlayerListener implements Listener {
    private final JavaPlugin plugin;
    private final DataManager dataManager;
    private final SpawnManager spawnManager;
    private final TablistManager tablistManager;
    private final BoardManager boardManager;

    public PlayerListener(JavaPlugin plugin, DataManager dataManager, SpawnManager spawnManager, TablistManager tablistManager, BoardManager boardManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.spawnManager = spawnManager;
        this.tablistManager = tablistManager;
        this.boardManager = boardManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);
        tablistManager.apply(event.getPlayer());
        boardManager.update(event.getPlayer());
        Location spawn = spawnManager.getSpawn();
        if (club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("spawn.teleport-on-join", false) && spawn != null && spawn.getWorld() != null) {
            event.getPlayer().teleport(spawn);
        }
        if (plugin instanceof Game game) {
            game.getAuctionHouseManager().sendPendingNotifications(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        tablistManager.remove(event.getPlayer());
        boardManager.remove(event.getPlayer());
        dataManager.save();
    }
}
