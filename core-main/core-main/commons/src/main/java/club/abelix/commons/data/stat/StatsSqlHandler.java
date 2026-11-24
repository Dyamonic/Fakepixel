package club.abelix.commons.data.stat;

import club.abelix.commons.data.punishment.BansSqlHandler;
import club.abelix.commons.mysql.modern.BufferedQuery;
import club.abelix.commons.mysql.modern.BufferedRow;
import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import club.abelix.commons.util.object.NumberPair;
import club.abelix.commons.util.time.TimeUtil;
import club.abelix.network.common.logging.Logging;
import club.abelix.network.type.mode.ServerMode;
import club.abelix.network.type.mode.Special;
import club.abelix.network.type.mode.Unknown;
import club.abelix.network.util.ModeUtil;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class StatsSqlHandler {

    public static final StatsSqlHandler INSTANCE = new StatsSqlHandler();

    private final Database database = new MySQL(DatabaseCredentials.MAIN.withDatabase("stats"));

    /**
     * Получить статистику игрока на режиме
     *
     * @param mode     - режим статистики
     * @param playerId - id игрока
     * @param fromDay  - время начала (кол-во дней)
     * @return - статистика игрока
     */
    public CompletableFuture<TIntObjectMap<TObjectIntMap<StatsField>>> loadPlayerStats(
            @NonNull StatsMode mode, int playerId, int fromDay) {

        return database.executeQuery(mode.getSelectQuery(), playerId, fromDay).thenApply(rows -> {
            TIntObjectMap<TObjectIntMap<StatsField>> stats = new TIntObjectHashMap<>();

            for (BufferedRow row : rows) {
                TObjectIntMap<StatsField> dayStats = new TObjectIntHashMap<>();

                for (StatsField statsField : mode.getFields()) {
                    dayStats.put(statsField, row.getRequiredInt(statsField.getName()));
                }

                stats.put(dayStats.get(mode.getDay()), dayStats);
            }

            return stats;
        });
    }

    /**
     * Добавить значение к текущему в статистике
     *
     * @param field    - поле, для которого добавлять значение
     * @param playerId - id игрока
     * @param day      - день за который добавлять статистику
     * @param value    - значение, которое добавлять
     */
    public CompletableFuture<?> addPlayerStatValue(
            @NonNull StatsField field, int playerId, int day, int value) {

        return database.executeVoidUpdate(field.getAddPlayerStatQuery(),
                playerId, day, value, value);
    }

    // Tables and Fields

    /**
     * Загрузить все таблицы из базы данных статистики
     *
     * @return - список таблиц
     */
    CompletableFuture<Set<String>> loadTables() {
        return database.executeQuery(
                "SELECT TABLE_NAME FROM information_schema.tables WHERE table_schema = 'stats';"
        ).thenApply(rows -> {
            Set<String> tables = new HashSet<>();

            for (BufferedRow row : rows) {
                String name = row.getRequiredString("TABLE_NAME");
                ServerMode mode = ModeUtil.getServerModeByName(name);

                if (mode instanceof Unknown || mode instanceof Special) {
                    continue;
                }

                tables.add(name);
            }

            return tables;
        });
    }

    /**
     * Загрузить режим статистики по названию таблицы
     *
     * @param table - название таблицы в БД
     * @return - режим статистики
     */
    CompletableFuture<StatsMode> loadMode(@NonNull String table) {
        return database.executeQuery(String.format(
                "SELECT * FROM information_schema.columns WHERE table_schema = 'stats' AND table_name = '%s';",
                table)).thenApply(rows -> {
            StatsMode mode = new StatsMode(table);

            for (BufferedRow row : rows) {
                String type = row.getRequiredString("COLUMN_TYPE");
                String name = row.getRequiredString("COLUMN_NAME");

                //игнорим столбцы с не-числовыми значениями и id игроков
                if (!type.contains("int") || name.contains("Id")) {
                    continue;
                }

                mode.addField(new StatsField(mode, name, row.getRequiredString("COLUMN_COMMENT")));
                Logging.IMP.info("[Statistic] Added field: " + name + " to mode: " + table);
            }

            mode.buildSelectQuery();
            Logging.IMP.info("[Statistic] Built select query for mode: " + table);

            return mode;
        });
    }

    public CompletableFuture<List<NumberPair>> getTemporaryTop(
            @NonNull StatsField field, int fromDay, int limit) {

        return getTemporaryTop(field, fromDay, limit, false);
    }

    /**
     * Получить топ за какой-то промежуток времени
     *
     * @param field     - поле топа
     * @param fromDay   - старт промежутка (стартовый день)
     * @param limit     - максимальное количество
     * @param ascending - режим сортировки (false = DESC, true = ASC)
     * @return - топ игроков
     */
    public CompletableFuture<List<NumberPair>> getTemporaryTop(
            @NonNull StatsField field, int fromDay, int limit, boolean ascending) {

        String function;

        switch (field.getTemporaryDisplayMode()) {
            case "max": {
                function = "MAX";
                break;
            }
            case "min": {
                function = "MIN";
                break;
            }
            default: {
                function = "SUM";
            }
        }

        return database.executeQuery(field.getTopQuery(function, ascending), fromDay)
                .thenApply(rows -> processPlayerTop(limit, rows));
    }

    private List<NumberPair> processPlayerTop(int limit, BufferedQuery rows) {
        List<NumberPair> bestPlayers = new ArrayList<>(limit);

        for (BufferedRow row : rows) {
            int playerId = row.getRequiredInt("Id");

            if (BansSqlHandler.INSTANCE.isBanned(playerId).join()) {
                continue;
            }

            bestPlayers.add(
                    NumberPair.newImmutablePair(
                            playerId,
                            row.getRequiredInt("Total")
                    )
            );

            if (bestPlayers.size() == limit) {
                break;
            }
        }

        return bestPlayers;
    }

    public CompletableFuture<List<NumberPair>> getTopAllTime(@NonNull StatsField field, int limit) {
        return getTopAllTime(field, limit, false);
    }

    /**
     * Получить топ за все время
     *
     * @param field     - поле топа
     * @param limit     - максимальное количество
     * @param ascending - режим сортировки (false = DESC, true = ASC)
     * @return - топ игроков
     */
    public CompletableFuture<List<NumberPair>> getTopAllTime(
            @NonNull StatsField field, int limit, boolean ascending) {

        return database.executeQuery(field.getAllTimeTopQuery(ascending))
                .thenApply(rows -> processPlayerTop(limit, rows));
    }

    public void deleteStats(int playerId, @NotNull StatsMode mode) {
        deleteStats(playerId, mode, 0);
    }

    public void deleteStats(int playerId, @NotNull StatsMode mode, int fromDay) {
        if (fromDay > 0) {
            int today = (int) TimeUtil.getDayUTC();

            TIntObjectMap<TObjectIntMap<StatsField>> stats
                    = loadPlayerStats(mode, playerId, fromDay).join();

            TObjectIntMap<StatsField> overall = stats.get(0);

            if (overall != null) {
                for (StatsField field : mode.getFields()) {
                    int overallValue = overall.get(field);

                    for (int day = fromDay; day <= today; day++) {
                        TObjectIntMap<StatsField> daily = stats.get(day);

                        if (daily == null) {
                            continue;
                        }

                        overallValue -= daily.get(field);

                        if (overallValue < 0) {
                            overallValue = 0;
                            break;
                        }
                    }

                    database.executeVoidUpdate(String.format("UPDATE `stats`.`%s` SET `%s` = ? WHERE `Id` = ? AND `day` = 0", mode.getTable(), field.getName()),
                            overallValue, playerId);
                }
            }
        }

        database.executeVoidUpdate(String.format("DELETE FROM `stats`.`%s` WHERE `Id` = ? AND `day` >= ?", mode.getTable()), playerId, fromDay);
    }

}
