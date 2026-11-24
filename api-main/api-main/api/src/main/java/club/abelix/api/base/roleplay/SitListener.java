package club.abelix.api.base.roleplay;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class SitListener extends YListener {

    public SitListener(AbstractPlugin plugin) {
        super(plugin);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Gamer gamer = Gamer.getGamer(event.getPlayer());

        SitManager.INSTANCE.eject(gamer);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Gamer gamer = Gamer.getGamer(event.getPlayer());
        if (gamer == null) return;

        SitManager.INSTANCE.eject(gamer);
    }


}
