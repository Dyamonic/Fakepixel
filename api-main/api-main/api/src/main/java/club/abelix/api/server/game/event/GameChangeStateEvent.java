package club.abelix.api.server.game.event;

import club.abelix.api.server.game.Game;
import club.abelix.network.common.enums.GameState;
import lombok.Getter;

@Getter
public class GameChangeStateEvent extends GameEvent {
    private final GameState oldState;
    private final GameState newState;

    public GameChangeStateEvent(Game instance, GameState oldState, GameState newState) {
        super(instance);
        this.oldState = oldState;
        this.newState = newState;
    }

}
