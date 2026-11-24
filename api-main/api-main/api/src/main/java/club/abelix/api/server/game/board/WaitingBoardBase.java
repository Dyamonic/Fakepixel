package club.abelix.api.server.game.board;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.scoreboard.Board;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.setting.GameSetting;
import club.abelix.commons.language.Language;
import com.google.common.base.Strings;
import lombok.NonNull;

public abstract class WaitingBoardBase extends Board {

    protected final Game game;
    protected final Gamer gamer;
    protected final Language language;

    protected int TIMER_LINE = 7;

    public WaitingBoardBase(@NonNull Game game, @NonNull Gamer gamer) {
        super(game.getPlugin());

        this.game = game;
        this.gamer = gamer;
        this.language = gamer.getLanguage();

        initScoreBoard();
        addPlayer(gamer.getPlayer());
    }

    // Override this method, and add the lines you want
    public abstract void initScoreBoard();

    public void updateStartTime(int time) {
        if (!game.getWaitingManager().isStarted()) {
            return;
        }

        setLine(TIMER_LINE, language.getMessage("board_until_start", "%time%", time + "s"));
    }


    public void addBaseUpdater(int playerCounterLine) {

        update(new Runnable() {

            private int dots = 0;

            @Override
            public void run() {
                setLine(playerCounterLine, language.getMessage("board_players", "%players%",
                        game.getNotVanishedOnline()
                                + "/" + game.getSetting(GameSetting.MAX_SLOTS, 0)));

                if (!game.getWaitingManager().isStarted()) {
                    setLine(TIMER_LINE, language.getMessage("board_waiting") + Strings.repeat(".", dots));

                    dots = dots == 3 ? 0 : dots + 1;
                }
            }
        }, 20, true);

    }

}
