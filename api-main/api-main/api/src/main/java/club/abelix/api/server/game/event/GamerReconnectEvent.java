package club.abelix.api.server.game.event;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.game.Game;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GamerReconnectEvent extends GameEvent {

    Gamer gamer;

    public GamerReconnectEvent(
            final Game game,
            final Gamer gamer
    ) {
        super(game);

        this.gamer = gamer;
    }

}
