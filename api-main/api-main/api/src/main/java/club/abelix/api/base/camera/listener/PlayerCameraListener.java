package club.abelix.api.base.camera.listener;

import club.abelix.api.base.camera.CameraDestroyReason;
import club.abelix.api.base.camera.PlayerCameraManager;
import club.abelix.api.base.listener.YListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public class PlayerCameraListener extends YListener {

    public PlayerCameraListener(Plugin plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        PlayerCameraManager.IMP.removeCamera(event.getPlayer(), CameraDestroyReason.PLAYER_QUIT);
    }

}
