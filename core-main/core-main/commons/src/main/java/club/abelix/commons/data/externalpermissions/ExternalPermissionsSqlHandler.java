package club.abelix.commons.data.externalpermissions;

import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import club.abelix.commons.util.time.TimeUtil;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>ExternalPermissionsSqlHandler creation on 14.01.2023 at 19:20</p>
 */

public class ExternalPermissionsSqlHandler {

    public static final ExternalPermissionsSqlHandler INSTANCE = new ExternalPermissionsSqlHandler();

    /**
     * Запрос на загрузку данных
     */
    private static final String LOAD_PERMISSION_ENTRY = "SELECT * FROM `ExternalPermissions` WHERE `Gamer` = ?;";

    /**
     * Запрос на сохранение данных
     */
    private static final String SAVE_PERMISSION_ENTRY = "INSERT INTO `ExternalPermissions` (`Gamer`, `Permission`, `AddDay`, `Lobby`, `Mini`, `Uncommon`, `Special`) VALUES (?, ?, ?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE `Lobby` = ?, `Mini` = ?, `Uncommon` = ?, `Special` = ?;";

    /**
     * Запрос на удаление данных
     */
    private static final String REMOVE_PERMISSION_ENTRY = "DELETE FROM `ExternalPermissions` WHERE `Id` = ?";

    // -----------------------------------------------------------------

    private final Database database = new MySQL(DatabaseCredentials.MAIN.withDatabase("playerdata"));

    /**
     * Загрузить данные игрока по его id
     *
     * @param playerId - id игрока
     * @return - данные игрока
     */
    public CompletableFuture<ArrayList<ExternalPermissionEntry>> loadPlayerInfo(int playerId) {
        ArrayList<ExternalPermissionEntry> result = new ArrayList<>();

        return database.executeQuery(LOAD_PERMISSION_ENTRY, playerId).thenApply(rows -> {
            rows.forEach(bufferedRow -> result.add(new ExternalPermissionEntry(
                    bufferedRow.getRequiredInt("Id"),
                    bufferedRow.getRequiredInt("Gamer"),
                    bufferedRow.getRequiredString("Permission"),
                    bufferedRow.getBoolean("Lobby"),
                    bufferedRow.getBoolean("Mini"),
                    bufferedRow.getBoolean("Uncommon"),
                    bufferedRow.getBoolean("Special"))));
            return result;
        });
    }

    /**
     * Сохранить данные прав
     *
     * @param entry данные прав
     * @return в entry добавлено ID права
     */
    public CompletableFuture<ExternalPermissionEntry> savePermissionInfo(@NonNull ExternalPermissionEntry entry) {
        int currentDay = (int) TimeUtil.getDayUTC();
        return database.executeUpdate(SAVE_PERMISSION_ENTRY,
                entry.getGamerId(), entry.getPermission(), currentDay,
                entry.isCanLobby(), entry.isCanMini(), entry.isCanUncommon(), entry.isCanSpecial(),
                entry.isCanLobby(), entry.isCanMini(), entry.isCanUncommon(), entry.isCanSpecial()).thenApply(bufferedExecutionWithGeneratedKeys -> {
            entry.setPermissionId(bufferedExecutionWithGeneratedKeys.getGeneratedKey());
            return entry;
        });
    }

    public void removePermissionEntry(@NonNull ExternalPermissionEntry entry) {
        database.executeVoidUpdate(REMOVE_PERMISSION_ENTRY, entry.getPermissionId());
    }
}

