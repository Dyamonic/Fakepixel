package club.abelix.api.base.playertop.provider;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.playertop.entry.PlayerTopEntry;
import club.abelix.commons.data.property.PropertySqlHandler;
import gnu.trove.map.TIntIntMap;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Берет топ игроков по определенной проперти игрока
 */
public class PropertyTopProvider implements PlayerTopProvider {

    private final String propertyKey;
    private final int limit;
    private final boolean ascending;

    public PropertyTopProvider(@NonNull String propertyKey, int limit, boolean ascending) {
        this.propertyKey = propertyKey;
        this.limit = limit;
        this.ascending = ascending;
    }

    @Override
    public List<PlayerTopEntry> updatePlayerTop() {
        List<PlayerTopEntry> playerTopEntries = new ArrayList<>();

        TIntIntMap loadedPropertiesTop = PropertySqlHandler.INSTANCE
                .loadTopPlayerProperties(propertyKey, ascending, limit).join();

        loadedPropertiesTop.forEachEntry((playerId, value) -> {
            Gamer gamer = Gamer.getOrCreate(playerId, loadedGamer -> {
                loadedGamer.loadRanks();
                loadedGamer.loadPrefixAndUpdateName();
                loadedGamer.loadProperties();
                loadedGamer.loadSuffix();
            });

            if (gamer == null) {
                return false;
            }

            playerTopEntries.add(new PlayerTopEntry(gamer, value));

            return true;
        });

        Comparator<PlayerTopEntry> comparator = Comparator.comparingInt(PlayerTopEntry::getValue);

        if (!ascending) {
            comparator = comparator.reversed();
        }

        playerTopEntries.sort(comparator);

        return playerTopEntries;
    }
}
