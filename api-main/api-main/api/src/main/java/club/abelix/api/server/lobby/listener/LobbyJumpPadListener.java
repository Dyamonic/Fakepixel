package club.abelix.api.server.lobby.listener;

import club.abelix.api.base.listener.YListener;
import lombok.AllArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;

@AllArgsConstructor
public class LobbyJumpPadListener extends YListener {

    private final double multiply, yCord;
    private final Material material;

    @EventHandler
    public void onPlayerFallAndMultiply(PlayerMoveEvent event) {
        Location location = event.getTo();
        Player player = event.getPlayer();

        if (player.isOnGround() && location.getBlock().getType() == material) {
            player.setVelocity(location.getDirection().multiply(multiply).setY(yCord));
        }
    }
}
