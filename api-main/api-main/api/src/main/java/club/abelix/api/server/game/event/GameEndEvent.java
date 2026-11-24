package club.abelix.api.server.game.event;

import club.abelix.api.server.game.Game;

/**
 * Written by Vlundik for LiteAPI in 03 2016
 */
public class GameEndEvent extends GameEvent {

    // Кидается после того, как установлено состояние ENDING

    public GameEndEvent(Game instance) {
        super(instance);
    }

}
