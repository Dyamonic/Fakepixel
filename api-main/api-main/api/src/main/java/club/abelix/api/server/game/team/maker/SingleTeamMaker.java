package club.abelix.api.server.game.team.maker;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.game.Game;
import lombok.NonNull;

import java.util.List;

/**
 * Created by saharNooby on 1:08 19.10.2017
 */
public class SingleTeamMaker implements TeamMaker {

    public static final SingleTeamMaker INSTANCE = new SingleTeamMaker();

    // Засовывает каждого игрока в свою тиму

    @Override
    public void make(@NonNull Game game, @NonNull List<Gamer> gamers) {
        for (Gamer gamer : gamers) {
            createTeam(game).add(gamer);
        }
    }

}
