package club.abelix.api.server.game.event;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.game.Game;
import lombok.Getter;
import lombok.NonNull;

@Getter
public class GameQuitEvent extends GameEvent {

    private final Gamer gamer;
    private final boolean change;

    public GameQuitEvent(@NonNull Game game, Gamer gamer, boolean change) {
        super(game);

        this.gamer = gamer;
        this.change = change;
    }
}
