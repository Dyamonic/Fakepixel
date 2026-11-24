package club.abelix.api.server.lobby.portal;

import club.abelix.api.base.listener.YListener;
import club.abelix.commons.util.time.CooldownUtil;
import club.abelix.network.type.connect.ConnectStatus;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public abstract class AbstractPortalListener extends YListener {

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        if (to.getBlock().getType() != Material.ENDER_PORTAL) {
            return;
        }

        Player player = event.getPlayer();

        String cooldownId = player.getName().concat("/Portal");

        if (CooldownUtil.hasCooldown(cooldownId)) {
            return;
        }

        CooldownUtil.addCooldown(cooldownId, 1_200, TimeUnit.MILLISECONDS);

        connect(player).whenComplete((status, cause) -> {
            if (status != ConnectStatus.SUCCESS) {
                giveVelocity(player);
            }
        });
    }

    private void giveVelocity(@NonNull Player player) {
        Vector vector = player.getLocation().getDirection().multiply(-2);
        vector.setY(0.8);

        player.setVelocity(vector);
    }

    protected abstract CompletableFuture<ConnectStatus> connect(@NonNull Player player);

}
