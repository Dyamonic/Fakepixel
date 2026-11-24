package club.abelix.commons.data.dailyrewards;

import club.abelix.commons.mysql.modern.BufferedRow;
import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import club.abelix.commons.util.time.TimeUtil;
import lombok.NonNull;

import java.util.concurrent.CompletableFuture;

/**
 * Ежедневные награды за вход подряд
 */
public class DailyRewardSqlHandler {

    public static final DailyRewardSqlHandler INSTANCE = new DailyRewardSqlHandler();

    /**
     * Запрос на загрузку данных
     */
    private static final String LOAD_PLAYER_ENTRY = "SELECT `DayStart`, `LastDay` FROM `playerdata`.`DailyRewards` WHERE `GamerId` = ?;";

    /**
     * Запрос на сохранение данных
     */
    private static final String SAVE_PLAYER_ENTRY = "INSERT INTO `playerdata`.`DailyRewards` (`GamerId`, `DayStart`, `LastDay`) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `DayStart` = ?, `LastDay` = ?;";

    /**
     * Запрос на удаление данных
     */
    private static final String REMOVE_PLAYER_ENTRY = "DELETE FROM `playerdata`.`DailyRewards` WHERE `GamerId` = ?";

    // -----------------------------------------------------------------

    private final Database database = new MySQL(DatabaseCredentials.MAIN.withDatabase("playerdata"));

    /**
     * Загрузить данные игрока по его id
     *
     * @param playerId - id игрока
     * @return - данные игрока
     */
    public CompletableFuture<DailyRewardEntry> loadPlayerInfo(int playerId) {
        int currentDay = (int) TimeUtil.getDayUTC();

        return database.executeQuery(LOAD_PLAYER_ENTRY, playerId).thenApply(rows -> {
            if (rows.isEmpty()) {
                return new DailyRewardEntry(playerId, 0, 0);
            }

            BufferedRow row = rows.getLast();

            int dayStart = row.getRequiredInt("DayStart");
            int lastDay = row.getRequiredInt("LastDay");

            //если игрок не заходил в предыдущий день или уже получил кучу наград
            if (currentDay - lastDay > 1) {
                database.executeVoidUpdate(REMOVE_PLAYER_ENTRY, playerId).join();
                return new DailyRewardEntry(playerId, 0, 0);
            }

            return new DailyRewardEntry(playerId, dayStart, lastDay);
        });
    }

    /**
     * Сохранить данные игрока по типу награды
     *
     * @param entry данные игрока
     */
    public void savePlayerInfo(@NonNull DailyRewardEntry entry) {
        database.executeVoidUpdate(SAVE_PLAYER_ENTRY,
                entry.getPlayerId(), entry.getDayStart(), entry.getLastDay(), entry.getDayStart(), entry.getLastDay());
    }
}
