package club.abelix.api.base.report.listener;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import club.abelix.api.base.report.gui.player.ReportGui;
import club.abelix.api.base.report.gui.staff.ReportListGui;
import club.abelix.commons.data.permission.Group;
import club.abelix.core.bukkit.connection.Connection;
import club.abelix.network.packet.play.client.CReportCheckExistenceRequest;
import lombok.NonNull;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

@Deprecated(forRemoval = true)
public class ReportListener extends YListener {

    public ReportListener(@NonNull AbstractPlugin plugin) {
        super(plugin);
    }

    @EventHandler
    public void onPlayerInteractAtEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof Player target)) {
            return;
        }

        if (!player.isSneaking()) {
            return;
        }

        Gamer gamer = Gamer.getGamer(player);
        Gamer targetGamer = Gamer.getGamer(target);

        if (gamer == null || targetGamer == null || !gamer.getRanks().hasPermission("report.view")) {
            return;
        }

        Connection.awaitPacket(new CReportCheckExistenceRequest(targetGamer.getPlayerId()))
                .whenComplete((response, cause) -> {
                    if (response.isExists()) {
                        new ReportListGui(gamer.getPlayer());
                    }
                });
    }

}
