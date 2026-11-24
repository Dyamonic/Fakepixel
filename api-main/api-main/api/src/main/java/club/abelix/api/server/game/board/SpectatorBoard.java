package club.abelix.api.server.game.board;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.scoreboard.Board;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.setting.GameSetting;
import club.abelix.commons.language.Language;
import lombok.NonNull;

public class SpectatorBoard extends Board {

    public SpectatorBoard(@NonNull Game game, @NonNull Gamer gamer) {
        super(game.getPlugin());

        Language language = gamer.getLanguage();

        setAnimatedDisplayName(language.getMessage("board_spectator_display_name"));

        setBoardWebsite(0, language);
        setLine(1);
        setBoardServer(2, language, game.getNameDisplay());

        if (game.getSetting(GameSetting.ENABLE_WORLD_NAME_DISPLAY, true)) {
            setBoardMap(3, language, game.getDisplayMapName());
        }

        setLine(4);
        //наблюдатели
        //игроки
        setLine(7);
        // время

        setBoardDateTime(8, language, game.getMini());

        update(() -> {
            setLine(5, language.getMessage("board_spectator_spectators", "%spectators%", game.getSpectators().size()));
            setLine(6, language.getMessage("board_players", "%players%", game.getAlivePlayers().size()));
        }, 40, true);

        addPlayer(gamer.getPlayer());
    }

}
