package club.abelix.api.server.game.listener.ending;

import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.listener.GameListener;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;

public class GameEndListener extends GameListener<Game> {
    public GameEndListener(Game game) {
        super(game);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        checkGame(event, () -> {
            Location to = event.getTo();

            if (to.getY() < 0) {
                event.setTo(game.getSpawnLocation());
            }
        });
    }

}
