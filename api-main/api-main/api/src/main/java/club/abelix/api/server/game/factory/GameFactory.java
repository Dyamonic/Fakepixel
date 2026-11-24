package club.abelix.api.server.game.factory;

import club.abelix.api.server.game.Game;
import club.abelix.network.type.mode.Mini;

public interface GameFactory {

    Game createGame(Mini mini);

}
