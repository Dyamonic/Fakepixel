package club.abelix.api.server.base.listener;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.gamer.event.GamerJoinEvent;
import club.abelix.api.base.listener.YListener;
import club.abelix.commons.data.permission.Group;
import lombok.NonNull;
import lombok.val;
import org.bukkit.event.EventHandler;

public class DevModeListener  extends YListener {

    private static final String DEV_MODE_PROPERTY = "Dev/Mode";

    public DevModeListener(@NonNull AbstractPlugin plugin) {
        super(plugin);
    }

    @EventHandler
    public void onJoin(GamerJoinEvent event) {
        val gamer = event.getGamer();
        int currentStatus = event.getGamer().getProperty(DEV_MODE_PROPERTY);

        if (currentStatus == 1 && !gamer.getRanks().hasPermission("dev.mode")) {
            gamer.setProperty(DEV_MODE_PROPERTY, 0);
            gamer.getPlayer().setOp(false);
        }
    }
}
