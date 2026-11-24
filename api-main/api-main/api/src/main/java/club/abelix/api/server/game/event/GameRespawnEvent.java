package club.abelix.api.server.game.event;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.game.Game;
import org.bukkit.entity.Player;

public class GameRespawnEvent extends GameEvent {

    private final Gamer gamer;

    public GameRespawnEvent(Game game, Gamer gamer) {
        super(game);

        this.gamer = gamer;
    }

    public Gamer getGamer() {
        return gamer;
    }

    public Player getPlayer() {
        return gamer.getPlayer();
    }
}
