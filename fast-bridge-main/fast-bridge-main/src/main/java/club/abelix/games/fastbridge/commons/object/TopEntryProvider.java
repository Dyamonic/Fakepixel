package club.abelix.games.fastbridge.commons.object;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.playertop.entry.PlayerTopEntry;
import club.abelix.api.base.playertop.provider.PlayerTopProvider;
import club.abelix.commons.data.stat.StatsSqlHandler;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.object.NumberPair;
import club.abelix.games.fastbridge.commons.utils.RecordFormat;
import club.abelix.games.fastbridge.game.FastBridgeGame;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class TopEntryProvider implements PlayerTopProvider {

    String worldName;

    @Override
    public String format(PlayerTopEntry entry, Language language) {
        return language.getMessage("fastbridge_top_format",
                "%name%",
                entry.getGamer().getDisplayName(),
                "%value%",
                RecordFormat.formatTime(entry.getValue()));
    }

    @Override
    public List<PlayerTopEntry> updatePlayerTop() throws Exception {
        val top = StatsSqlHandler.INSTANCE
                .getTopAllTime(FastBridgeGame.getStatsMode().getField(worldName), 10, true)
                .join();
        List<PlayerTopEntry> playerTopEntries = new ArrayList<>();

        for (NumberPair topEntry : top) {
            Gamer gamer = Gamer.getOrCreate(topEntry.getFirstInt(), loadedGamer -> {
                loadedGamer.loadRanks();
                loadedGamer.loadPrefixAndUpdateName();
                loadedGamer.loadSuffix();
            });

            playerTopEntries.add(new PlayerTopEntry(gamer, topEntry.getSecondInt()));
        }

        playerTopEntries.sort(Comparator.comparingInt((PlayerTopEntry entry) -> entry.getValue() == 0 ? 1 : 0)
                .thenComparingInt(PlayerTopEntry::getValue));

        return playerTopEntries;
    }
}
