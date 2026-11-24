package club.abelix.api.server.game.stats;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.event.GameStatsChangeEvent;
import club.abelix.commons.data.stat.StatsField;
import club.abelix.commons.data.stat.StatsSqlHandler;
import club.abelix.commons.util.time.TimeUtil;
import club.abelix.core.bukkit.HeartBeat;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class GameStatsCache {

    // общая кешированная стата за эту сессию
    @Getter
    private final TIntObjectMap<TObjectIntMap<StatsField>> cachedStats = new TIntObjectHashMap<>();
    // еще несохраненная в базу стата за эту сессию
    @Getter
    private final TIntObjectMap<TObjectIntMap<StatsField>> unsavedStats = new TIntObjectHashMap<>();

    // playerId и playerName хранятся отдельно, чтобы save() работал без сильной ссылки на Gamer
    @Getter
    private final int playerId;
    private final String playerName;

    @Getter
    private Gamer gamer;

    // если уже сохранена - не будет сохраняться при окончании игры
    @Getter
    private boolean saved;

    public GameStatsCache(@NonNull Gamer gamer) {
        this.gamer = gamer;
        this.playerId = gamer.getPlayerId();
        this.playerName = gamer.getName();
    }

    public void rejoin(Gamer gamer) {
        if (this.playerId == gamer.getPlayerId()) {
            this.gamer = gamer;
        }
    }

    public TObjectIntMap<StatsField> getCachedStats(int day) {
        TObjectIntMap<StatsField> cachedStats = this.cachedStats.get(day);

        if (cachedStats == null) {
            this.cachedStats.put(day, cachedStats = new TObjectIntHashMap<>());
        }

        return cachedStats;
    }

    public TObjectIntMap<StatsField> getUnsavedStats(int day) {
        TObjectIntMap<StatsField> unsavedStats = this.unsavedStats.get(day);

        if (unsavedStats == null) {
            this.unsavedStats.put(day, unsavedStats = new TObjectIntHashMap<>());
        }

        return unsavedStats;
    }

    public int getCachedStat(@NonNull StatsField statsField, int day) {
        return getCachedStats(day).get(statsField);
    }

    public void addStat(@NonNull StatsField field, int add, int day) {
        getCachedStats(day).adjustOrPutValue(field, add, add);

        Gamer g = this.gamer;
        if (g == null) {
            return;
        }

        Game game = g.getGame();

        // стата не сохраняется в этой игре, либо этого поля нет в базе
        if ((game != null && game.isStatsSavingDisabled())
                || !field.getMode().getFields().contains(field)) {
            return;
        }

        getUnsavedStats(day).adjustOrPutValue(field, add, add);

        if (day == TimeUtil.getDayUTC()) {
            HeartBeat.callEvent(new GameStatsChangeEvent(g, field, add, g.getTodayStat(field)));
        }
    }

    public void mergeFrom(@NonNull GameStatsCache anotherCache) {
        mergeStats(cachedStats, anotherCache.cachedStats);
        mergeStats(unsavedStats, anotherCache.unsavedStats);
        saved = saved && anotherCache.saved;
    }

    private static void mergeStats(TIntObjectMap<TObjectIntMap<StatsField>> target,
                                   TIntObjectMap<TObjectIntMap<StatsField>> source) {
        for (int day : source.keys()) {
            TObjectIntMap<StatsField> sourceStats = source.get(day);
            TObjectIntMap<StatsField> targetStats = target.get(day);

            if (targetStats == null) {
                target.put(day, sourceStats);
                continue;
            }

            sourceStats.forEachEntry((field, value) -> {
                targetStats.adjustOrPutValue(field, value, value);
                return true;
            });
        }
    }

    public void save() {
        save(false);
    }

    public void save(boolean sync) {
        for (int day : unsavedStats.keys()) {
            unsavedStats.get(day).forEachEntry((field, value) -> {
                CompletableFuture<?> future = StatsSqlHandler.INSTANCE
                        .addPlayerStatValue(field, playerId, day, value);

                if (sync) {
                    future.join();
                }

                return true;
            });
        }

        saved = true;

        log.info("Saving game stats of {} (entries: {})", playerName, unsavedStats.size());

        unsavedStats.clear();

        // Обнуляем ссылку на Gamer, чтобы не удерживать Player → World в памяти
        gamer = null;
    }

}
