package club.abelix.api.server.lobby.impl.hub.listener;

import club.abelix.api.base.camera.CameraDestroyReason;
import club.abelix.api.base.camera.event.CameraDestroyEvent;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import club.abelix.commons.data.level.Leveling;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.Plugin;

public class HubTutorialListener extends YListener {

    public HubTutorialListener(Plugin plugin) {
        super(plugin);
    }

    @EventHandler
    public void onTutorialEnd(CameraDestroyEvent event) {
        if (event.getReason() != CameraDestroyReason.CUSTOM) {
            return;
        }

        Player player = event.getCamera().getPlayer();
        Gamer gamer = Gamer.getGamer(player);

        if (gamer == null || gamer.getLevel() > 1) {
            return;
        }

        gamer.addExperience(Leveling.getTotalExpToLevel(2) - gamer.getExperience(), null);
        gamer.sendLocalizedMessage("tutorial_level_up");
    }

}
