package club.abelix.commons.data.rewards;

import club.abelix.commons.mysql.modern.BufferedRow;
import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import lombok.NonNull;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

/**
 * Награды за вход подряд
 */
public class RewardSqlHandler {

    public static final RewardSqlHandler INSTANCE = new RewardSqlHandler();

    /**
     * Запрос на загрузку данных
     */
    private static final String LOAD_PLAYER_ENTRY = "SELECT `Mouth`, `Group` FROM `playerdata`.`Rewards` WHERE `Id` = ? AND `Group` = ?;";

    /**
     * Запрос на сохранение данных
     */
    private static final String SAVE_PLAYER_ENTRY = "INSERT INTO `playerdata`.`Rewards` (`Id`, `Group`, `Mouth`) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `Group` = ?, `Mouth` = ?;";

    /**
     * Запрос на удаление данных
     */
    private static final String REMOVE_PLAYER_ENTRY = "DELETE FROM `playerdata`.`Rewards` WHERE `Id` = ?";

    // -----------------------------------------------------------------

    private final Database database = new MySQL(DatabaseCredentials.MAIN.withDatabase("playerdata"));

    /**
     * Загрузить данные игрока по его id
     *
     * @param playerId - id игрока
     * @return - данные игрока
     */
    public CompletableFuture<RewardEntry> loadPlayerInfo(int playerId, int groupId) {
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();

        return database.executeQuery(LOAD_PLAYER_ENTRY, playerId, groupId).thenApply(rows -> {
            if (rows.isEmpty()) {
                return new RewardEntry(playerId, groupId, -1);
            }

            BufferedRow row = rows.getLast();
            int lastMouth = row.getRequiredInt("Mouth");
            int group = row.getRequiredInt("Group");

            //если игрок не заходил в предыдущий день или уже получил кучу наград
            if (month - lastMouth >= 1 && lastMouth != -1) {
                database.executeVoidUpdate(REMOVE_PLAYER_ENTRY, playerId).join();
                return new RewardEntry(playerId, groupId, -1);
            }

            return new RewardEntry(playerId, group, lastMouth);
        });
    }

    /**
     * Сохранить данные игрока по типу награды
     *
     * @param entry данные игрока
     */
    public void savePlayerInfo(@NonNull RewardEntry entry) {
        database.executeVoidUpdate(SAVE_PLAYER_ENTRY,
                entry.playerId(), entry.groupId(), entry.mouth(), entry.groupId(), entry.mouth());
    }
}
