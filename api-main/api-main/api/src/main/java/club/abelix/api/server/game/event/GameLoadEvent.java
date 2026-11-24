package club.abelix.api.server.game.event;

import club.abelix.api.server.game.Game;

public class GameLoadEvent extends GameEvent {

    // Кидается перед началом загрузки игры

    public GameLoadEvent(Game instance) {
        super(instance);
    }
}
