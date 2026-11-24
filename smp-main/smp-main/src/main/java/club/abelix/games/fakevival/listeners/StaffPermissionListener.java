package club.abelix.games.fakevival.listeners;

import club.abelix.games.fakevival.managers.StaffPermissionManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class StaffPermissionListener implements Listener {
    private final StaffPermissionManager staffPermissionManager;

    public StaffPermissionListener(StaffPermissionManager staffPermissionManager) {
        this.staffPermissionManager = staffPermissionManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        staffPermissionManager.applyAsync(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        staffPermissionManager.revoke(event.getPlayer());
    }
}
