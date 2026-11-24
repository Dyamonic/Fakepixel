package club.abelix.commons.data.auth;

import club.abelix.commons.data.network.NetworkSqlHandler;
import club.abelix.commons.mysql.modern.BufferedExecutionWithGeneratedKeys;
import club.abelix.commons.mysql.modern.BufferedRow;
import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import lombok.NonNull;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AuthSqlHandler {

    public static final AuthSqlHandler INSTANCE = new AuthSqlHandler();

    /**
     * Запрос на получение данных об аккаунте
     */
    private static final String LOAD_PLAYER_DATA = "SELECT * FROM `NewAuth` WHERE `Name` = ?";

    /**
     * Запрос на изменение пароля
     */
    private static final String CHANGE_PASSWORD = "UPDATE `NewAuth` SET `password` = ? WHERE `Name` = ?;";

    /**
     * Запрос на изменение ВК игрока
     */
    private static final String CHANGE_VK_ID = "UPDATE `NewAuth` SET `vk_id` = ? WHERE `Name` = ?";

    /**
     * Запрос на обновление имени игрока
     */
    private static final String UPDATE_PLAYER_NAME = "UPDATE `NewAuth` SET `Name` = ? WHERE `Id` = ?";

    /**
     * Запрос на добавление данных об игроке
     */
    private static final String CREATE_PLAYER_DATA = "INSERT INTO `NewAuth` (`Name`, `password`, `reg_ip`, `reg_date`, `language`) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE `password` = ?, `reg_ip` = ?, `reg_date` = ?, `language`=?;";

    /**
     * Запрос на изменение статуса авторизации по лицензии
     */
    private static final String CHANGE_PREMIUM_STATUS = "UPDATE `NewAuth` SET `premium_auth` = ? WHERE `Id` = ?";

    /**
     * Запрос на получения статуса авторизации по лицензии
     */
    private static final String GET_PREMIUM_STATUS = "SELECT `premium_auth` FROM `NewAuth` WHERE `Name` = ?";

    /**
     * Запрос на получение аккаунтов по регистрационному IP
     */
    private static final String FIND_IP_ACCOUNTS = "SELECT `Name` FROM `NewAuth` WHERE `reg_ip` = ?";

    /**
     * Запрос на загрузку информации о привязанных аккаунтах по VK
     */
    private static final String GET_PLAYER_PASSWORD = "SELECT `password` FROM `NewAuth` WHERE `Name` = ?";

    /**
     * Запрос на установку/удаление телеграмма
     */
    private static final String SET_TELEGRAM = "UPDATE `NewAuth` SET `telegram` = ? WHERE `Id` = ?";

    private final Database database = new MySQL(DatabaseCredentials.MAIN.withDatabase("playerdata"));

    public CompletableFuture<Void> load(@NonNull BaseAuthPlayerData data) {
        String name = data.getName();

        return database.executeQuery(LOAD_PLAYER_DATA, name).thenAccept(rows -> {
            if (rows.isEmpty()) {
                return;
            }

            BufferedRow row = rows.getFirst();

            // id игрока
            data.setPlayerId(row.getRequiredInt("Id"));

            String currentName = row.getRequiredString("Name");

            // регистр букв в нике поменялся
            if (!currentName.equals(name)) {
                database.executeVoidUpdate(UPDATE_PLAYER_NAME, name, data.getPlayerId())
                        .join();
            }

            // пароль
            data.setPassword(row.getRequiredString("password"));

            // vk авторизация
            data.setVkId(row.getInt("vk_id").orElse(0));

            LocalDateTime regDate = row.getRequiredTimestamp("reg_date");
            ZonedDateTime zonedRegDate = regDate.atZone(ZoneId.systemDefault());

            // даты регистрации
            data.setRegDate(zonedRegDate.toInstant().toEpochMilli());

            // ip адрес регистрации
            data.setRegIp(row.getRequiredString("reg_ip"));

            data.setTelegramId(row.getRequiredLong("telegram"));

            // premium status
            data.setPremiumAuth(row.getBoolean("premium_auth"));

            // сохраняем ID для других эвентов (поскольку AuthPlayerData доступна только после входа)
            NetworkSqlHandler.INSTANCE.cachePlayerId(name, data.getPlayerId());
        });
    }

    public CompletableFuture<Integer> createAuth(@NonNull BaseAuthPlayerData data) {
        Timestamp regTimestamp = new Timestamp(data.getRegDate());
        int languageId = data.getLanguage().getId();

        return database.executeUpdate(CREATE_PLAYER_DATA, data.getName(), data.getPassword(), data.getRegIp(), regTimestamp, languageId, data.getPassword(), data.getRegIp(), regTimestamp, languageId)
                .thenApply(BufferedExecutionWithGeneratedKeys::getGeneratedKey);
    }

    public CompletableFuture<List<String>> findAccounts(@NonNull String regIp) {
        return database.executeQuery(FIND_IP_ACCOUNTS, regIp).thenApply(rows -> {
            List<String> accounts = new ArrayList<>(rows.size());

            for (BufferedRow row : rows) {
                accounts.add(row.getRequiredString("Name"));
            }

            return accounts;
        });
    }

    public void setPassword(@NonNull String name, @NonNull String password) {
        database.executeVoidUpdate(CHANGE_PASSWORD, password, name);
    }

    public void changeVkLink(@NonNull String name, int vkId) {
        database.executeVoidUpdate(CHANGE_VK_ID, vkId, name);
    }

    public void setPremiumStatus(int playerId, boolean status) {
        database.executeVoidUpdate(CHANGE_PREMIUM_STATUS, status, playerId);
    }

    public CompletableFuture<Boolean> isOnlineMode(@NonNull String name) {
        return database.executeQuery(GET_PREMIUM_STATUS, name)
                .thenApply(rows -> {
                    if (rows.isEmpty()) {
                        return false;
                    }

                    return rows.getFirst().getBoolean("premium_auth");
                });
    }

    public String getPlayerPassword(final @NonNull String playerName) {
        return database.executeQuery(GET_PLAYER_PASSWORD, playerName.toLowerCase())
                .thenApply(rows -> !rows.isEmpty()
                        ? rows.getFirst().getRequiredString("password")
                        : null
                )
                .join();
    }

    public void setTelegramId(int playerId, long telegramId) {
        database.executeVoidUpdate(SET_TELEGRAM, telegramId, playerId);
    }
}
