package club.abelix.api.server.game.board;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.scoreboard.Board;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.team.GameTeam;
import club.abelix.commons.language.Language;
import lombok.NonNull;

public abstract class EndBoardBase extends Board {

    protected final Game game;
    protected final Gamer gamer;
    protected final Language language;
    protected final GameTeam winner;

    public EndBoardBase(@NonNull Game game, @NonNull Gamer gamer, GameTeam winner) {
        super(game.getPlugin());

        this.game = game;
        this.gamer = gamer;
        this.language = gamer.getLanguage();
        this.winner = winner;

        initScoreBoard();
        addPlayer(gamer.getPlayer());
    }

    // Override this method, and add the lines you want
    public abstract void initScoreBoard();
}
