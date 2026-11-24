package club.abelix.api.server.game.board;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.setting.GameSetting;
import lombok.NonNull;

public class WaitingBoard extends WaitingBoardBase {

    public WaitingBoard(@NonNull Game game, @NonNull Gamer gamer) {
        super(game, gamer);
    }

    @Override
    public void initScoreBoard(){
        setAnimatedDisplayName(game.getMini().getMaster().getDisplayName());

        setLine(10);
        //игроки
        setLine(8);
        //ожидание с точечками
        setLine(4);

        if (game.<Boolean>getSetting(GameSetting.ENABLE_WORLD_NAME_DISPLAY, true)) {
            setBoardMap(3, language, game.getDisplayMapName());
        }

        if (game.<Boolean>getSetting(GameSetting.ENABLE_MINI_NAME_DISPLAY, false)) {
            setLine(5, language.getMessage("board_mode",
                    "%mode%", game.getMini().getShortName()));

            setLine(6);
        }

        setBoardMode(2, language, game.getMini().getShortName());

        setLine(1);
        setBoardWebsite(0, language);
        setBoardDateTime(11, language, game.getMini());

        addBaseUpdater(9);
    }
}
