package club.abelix.api.server.lobby.impl.lobby.board;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.api.base.util.scoreboard.Board;
import club.abelix.commons.data.stat.StatsManager;
import club.abelix.commons.data.stat.StatsMode;
import club.abelix.commons.language.Language;
import club.abelix.commons.language.LanguageContext;
import club.abelix.network.type.mode.Master;
import club.abelix.network.type.mode.Mini;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;

public abstract class LobbyBoard extends Board {

    public LobbyBoard(@NonNull Plugin plugin, @NonNull Gamer gamer, @NonNull StatLine[] lines, @Nullable Master master) {
        super(plugin);

        setAnimatedDisplayName(master == null ? "AGERA" : master.getDisplayName());

        Language language = gamer.getLanguage();

        setBoardWebsite(0, language);
        setLine(1);

        int i = 1;

        if (master != null) {
            for (StatLine line : lines) {
                setLine(++i, language.getMessage("board_lobby_stat_line",
                        "%name%", line.context.format(language),
                        "%value%", line.calculator.getPlayerStat(gamer, line.name)
                ));
            }

            setLine(++i, " " + language.getMessage("board_lobby_stat_title", "%master%", master.getDisplayName()));
            setLine(++i);
        }

        int plazmaLine = ++i; // плазма
        int coinsLine = ++i; // монетки

        setLine(++i, " " + language.getMessage("board_lobby_economy"));
        setLine(++i);

        update(() -> {
            setLine(plazmaLine, "  " + language.getMessage("board_gold",
                    "%gold%", gamer.getGold()));

            if (master != null) {
                setLine(coinsLine, "  " + language.getMessage("board_coins",
                        "%coins%", gamer.getCoins(master)));
            }
        }, 20 * 5, true);

        setBoardDateTime(i + 2, language);

        addPlayer(gamer.getPlayer());
    }

    public LobbyBoard(@NonNull Plugin plugin, @NonNull Gamer gamer, @NonNull StatLine... lines) {
        this(plugin, gamer, lines, CoreUtil.getMaster());
    }

    public interface StatisticCalculator {

        int getPlayerStat(@NonNull Gamer gamer, @NonNull String name);

    }

    @AllArgsConstructor
    public static class StatLine {

        private final LanguageContext context;

        private final String name;

        private final StatisticCalculator calculator;

        public StatLine(@NonNull LanguageContext context, @NonNull String name) {
            this(context, name, new MasterStatisticCalculator(CoreUtil.getMaster()));
        }

    }

    @RequiredArgsConstructor
    public static class MiniStatisticCalculator implements StatisticCalculator {

        private final Mini mini;

        @Override
        public int getPlayerStat(@NonNull Gamer gamer, @NonNull String name) {
            StatsMode statsMode = StatsManager.INSTANCE.tryGetMode(mini.name());

            if (statsMode != null) {
                return gamer.getLifetimeStat(statsMode, name);
            }

            return new MasterStatisticCalculator(mini.getMaster())
                    .getPlayerStat(gamer, name);
        }
    }

    @RequiredArgsConstructor
    public static class MasterStatisticCalculator implements StatisticCalculator {

        private final Master master;

        @Override
        public int getPlayerStat(@NonNull Gamer gamer, @NonNull String name) {
            StatsMode statsMode = StatsManager.INSTANCE.tryGetMode(master.name());

            return statsMode == null ? 0 : gamer.getLifetimeStat(statsMode, name);
        }
    }


}
