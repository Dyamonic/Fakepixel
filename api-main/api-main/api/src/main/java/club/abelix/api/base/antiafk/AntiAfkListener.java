package club.abelix.api.base.antiafk;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamer.event.GamerDisplayNameUpdateEvent;
import club.abelix.api.base.listener.YListener;
import club.abelix.commons.data.permission.Group;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class AntiAfkListener extends YListener {

    private final AntiAfk antiAfk;

    public AntiAfkListener(@NonNull AntiAfk antiAfk) {
        super(antiAfk.getPlugin());

        this.antiAfk = antiAfk;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        MovementData movementData = antiAfk.getMovementData(event.getPlayer());

        if (movementData == null) {
            return;
        }

        Location lastLocation = movementData.getLastLocation();
        Location toLocation = event.getTo();

        if (lastLocation == null
                || lastLocation.getBlockX() != toLocation.getBlockX()
                || lastLocation.getBlockZ() != toLocation.getBlockZ()) {

            movementData.setLastLocation(toLocation);
            movementData.setLastMovementTime(System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onPlayerLoad(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Gamer gamer = Gamer.getGamer(player);

        if (gamer.getRanks().hasPermission("antiafk.bypass")) {
            return;
        }

        antiAfk.setMovementData(event.getPlayer(), new MovementData(player.getLocation(), System.currentTimeMillis()));
    }

    @EventHandler
    public void onGroupChange(GamerDisplayNameUpdateEvent event) {
        Gamer gamer = event.getGamer();

        if (!gamer.getRanks().hasPermission("antiafk.bypass")) {
            return;
        }

        antiAfk.removeMovementData(gamer.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        antiAfk.removeMovementData(event.getPlayer());
    }


}
