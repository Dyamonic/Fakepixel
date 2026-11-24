package club.abelix.api.base.playertop.provider;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.playertop.entry.PlayerTopEntry;
import club.abelix.commons.data.stat.StatisticPeriod;
import club.abelix.commons.data.stat.StatsField;
import club.abelix.commons.data.stat.StatsSqlHandler;
import club.abelix.commons.util.object.NumberPair;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Берет топ игроков по определенному полю статистики
 * за определенный промежуток времени
 */
@RequiredArgsConstructor
public class StatisticTopProvider implements PlayerTopProvider {

    private final StatsField statsField;
    private final StatisticPeriod statisticPeriod;

    private final int limit;

    @Override
    public List<PlayerTopEntry> updatePlayerTop() {
        List<PlayerTopEntry> playerTopEntries = new ArrayList<>();

        List<NumberPair> playerList;

        if (statisticPeriod == StatisticPeriod.ALL) {
            playerList = StatsSqlHandler.INSTANCE.getTopAllTime(statsField, limit).join();
        } else {
            playerList = StatsSqlHandler.INSTANCE
                    .getTemporaryTop(statsField, statisticPeriod.getFromDay(), limit).join();
        }

        for (NumberPair topEntry : playerList) {
            Gamer gamer = Gamer.getOrCreate(topEntry.getFirstInt(), loadedGamer -> {
                loadedGamer.loadRanks();
                loadedGamer.loadPrefixAndUpdateName();
                loadedGamer.loadProperties();
                loadedGamer.loadSuffix();
            });

            playerTopEntries.add(new PlayerTopEntry(gamer, topEntry.getSecondInt()));
        }

        playerTopEntries.sort(Comparator.comparingInt(PlayerTopEntry::getValue));
        Collections.reverse(playerTopEntries);

        return playerTopEntries;
    }
}
