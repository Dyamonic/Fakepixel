package club.abelix.api.server.lobby.board;

import club.abelix.api.base.gamer.Gamer;

public interface BoardFactory {

    void createBoard(Gamer gamer);

}
