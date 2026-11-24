package club.abelix.api.server.game.board;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.team.GameTeam;
import club.abelix.commons.language.Language;
import lombok.NonNull;

public class EndBoard extends EndBoardBase {

    public EndBoard(@NonNull Game game, @NonNull Gamer gamer, GameTeam winner) {
        super(game, gamer, winner);
    }

    @Override
    public void initScoreBoard(){
        Language language = gamer.getLanguage();

        setAnimatedDisplayName(game.getMini().getMaster().getDisplayName());

        setBoardWebsite(0, language);
        setLine(1);
        setBoardServer(2, language, game.getNameDisplay());
        setLine(3);

        int lastLine = 4;

        if (winner == null) {
            setLine(lastLine++, language.getMessage("game_end_board_draw"));
        } else {
            for (Gamer target : winner.getPlayers()) {
                setLine(lastLine++, target.getDisplayName());
            }

            setLine(lastLine++, language.getMessage("game_end_board_winner"));
        }

        setLine(lastLine);
        setLine(lastLine + 1, language.getMessage("game_end_board_end"));

        setBoardDateTime(lastLine + 3, language);
    }

}
