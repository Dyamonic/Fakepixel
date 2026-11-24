package club.abelix.api.server.game.event;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.game.Game;
import lombok.Getter;
import lombok.NonNull;

@Getter
public class GameJoinEvent extends GameEvent {

    private final Gamer gamer;

    public GameJoinEvent(@NonNull Game game, Gamer gamer) {
        super(game);

        this.gamer = gamer;
    }
}
