package club.abelix.games.fakevival.listeners;

import club.abelix.games.fakevival.managers.RegionManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public final class RegionListener implements Listener {
    private final RegionManager regionManager;

    public RegionListener(RegionManager regionManager) {
        this.regionManager = regionManager;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockY() == event.getTo().getBlockY() && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;
        regionManager.checkMove(event.getPlayer());
    }
}
