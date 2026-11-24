package club.abelix.api.server.game.event;

import club.abelix.api.server.game.Game;

public class GameStartEvent extends GameEvent {

    // Кидается после того, как установлено состояние GAME

    public GameStartEvent(Game instance) {
        super(instance);
    }

}
