package club.abelix.api.server.game.event;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.listener.base.JoinResult;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GameConnectEvent extends GameEvent {

    private final Gamer gamer;
    private JoinResult result = JoinResult.SUCCESS;

    public GameConnectEvent(Game game, Gamer gamer) {
        super(true, game);

        this.gamer = gamer;
    }

    public void setResult(JoinResult result) {
        this.result = result;
    }
}
