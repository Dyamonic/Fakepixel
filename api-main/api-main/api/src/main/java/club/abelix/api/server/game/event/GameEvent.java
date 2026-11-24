package club.abelix.api.server.game.event;

import club.abelix.api.server.game.Game;
import club.abelix.core.bukkit.event.YEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public abstract class GameEvent extends YEvent {

    private final Game game;

    public GameEvent(boolean async, Game game) {
        super(async);

        this.game = game;
    }
}
