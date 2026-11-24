package club.abelix.commons.data.prefix;

import club.abelix.commons.mysql.modern.BufferedRow;
import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import lombok.NonNull;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

public class PrefixSqlHandler {

    private static final String GET_PREFIX = "SELECT * FROM `Prefixes` WHERE `Id` = ?;";
    private static final String SET_PREFIX = "INSERT INTO `Prefixes` (`Id`, `PrefixText`, `PrefixExpire`) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `PrefixText` = ? , `PrefixExpire` = ?;";
    private static final String UPDATE_PREFIX = "UPDATE `Prefixes` SET `PrefixText` = ? WHERE `Id` = ?;";
    private static final String REMOVE_PREFIX = "DELETE FROM `Prefixes` WHERE `Id` = ?;";
    public static PrefixSqlHandler INSTANCE = new PrefixSqlHandler();
    private final Database database = new MySQL(DatabaseCredentials.MAIN.withDatabase("playerdata"));

    /**
     * Получить префикс игрока по его id
     *
     * @param playerId - id игрока
     * @return - префикс игрока
     */
    public CompletableFuture<Prefix> getPrefix(int playerId) {
        return database.executeQuery(GET_PREFIX, playerId).thenApply(query -> {
            if (query.isEmpty()) {
                return null;
            }

            BufferedRow row = query.getFirst();

            return new Prefix(
                    row.getRequiredTimestamp("PrefixExpire"),
                    row.getRequiredString("PrefixText")
            );
        });
    }

    /**
     * Установить префикс игроку
     *
     * @param playerId   - id игрока
     * @param prefix     - префикс игрока
     * @param expireDate - срок действия (дата истечения)
     */
    public CompletableFuture<?> setPrefix(int playerId, @NonNull String prefix, @NonNull LocalDateTime expireDate) {
        return database.executeVoidUpdate(SET_PREFIX, playerId, prefix, expireDate, prefix, expireDate);
    }

    /**
     * Обновить префикс игрока
     *
     * @param playerId - id игрока
     * @param text     - префикс игрока
     */
    public CompletableFuture<?> updatePrefix(int playerId, String text) {
        return database.executeVoidUpdate(UPDATE_PREFIX, text, playerId);
    }

    /**
     * Удалить кастомный префикс игрока
     *
     * @param playerId - id игрока
     */
    public CompletableFuture<?> removePrefix(int playerId) {
        return database.executeVoidUpdate(REMOVE_PREFIX, playerId);
    }

}
