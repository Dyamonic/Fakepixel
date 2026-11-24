package club.abelix.api.server.game.team.maker;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.team.GameTeam;
import lombok.NonNull;

import java.util.List;

public interface TeamMaker {

    // Создаёт тимы из указанного списка геймеров
    // Реализация должна вызывать add(gamer) у созданных экземпляров GameTeam

    void make(@NonNull Game game, @NonNull List<Gamer> gamers);

    default GameTeam createTeam(@NonNull Game game) {
        return new GameTeam(game);
    }

}
