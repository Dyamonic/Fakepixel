package club.abelix.api.server.game.team.maker;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.game.Game;
import club.abelix.network.common.logging.Logging;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.util.List;

@RequiredArgsConstructor
public class SingleTeamWithEmptyMaker implements TeamMaker {
    private final int maxTeams;

    @Override
    public void make(@NonNull Game game, @NonNull List<Gamer> gamers) {
        int createdTeams = 0;

        for (Gamer gamer : gamers) {
            createTeam(game).add(gamer);
            createdTeams += 1;
        }

        if (createdTeams < maxTeams) {
            int needSize = maxTeams - createdTeams;
            Logging.IMP.info("[GameTeam] Need crate {} empty teams", needSize);

            for (int i = 0; i < needSize; i++) {
                val emptyTeam = createTeam(game);
                game.getTeamManager().addTeam(emptyTeam);
            }
        }
    }
}
