package club.abelix.games.fakevival.listeners;

import club.abelix.games.fakevival.managers.BackManager;
import club.abelix.games.fakevival.managers.HomeManager;
import club.abelix.games.fakevival.managers.SpawnManager;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public final class BackListener implements Listener {
    private final BackManager backManager;
    private final SpawnManager spawnManager;
    private final HomeManager homeManager;

    public BackListener(BackManager backManager, SpawnManager spawnManager, HomeManager homeManager) {
        this.backManager = backManager;
        this.spawnManager = spawnManager;
        this.homeManager = homeManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        backManager.setBackLocation(event.getEntity(), event.getEntity().getLocation());
        event.getEntity().sendMessage(CC.color("&eUse &f/back &eto return to where you died."));
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (hasVanillaRespawn(event)) return;
        if (!homeManager.getHomes(event.getPlayer()).isEmpty()) return;

        Location spawn = spawnManager.getSpawn();
        if (spawn != null && spawn.getWorld() != null) event.setRespawnLocation(spawn);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getFrom().getWorld() == null || event.getTo().getWorld() == null) return;
        if (event.getFrom().getWorld().equals(event.getTo().getWorld())
                && event.getFrom().distanceSquared(event.getTo()) < 1.0D) return;
        backManager.setBackLocation(event.getPlayer(), event.getFrom());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        backManager.remove(event.getPlayer());
    }

    private boolean hasVanillaRespawn(PlayerRespawnEvent event) {
        return event.isBedSpawn() || event.isAnchorSpawn();
    }
}
