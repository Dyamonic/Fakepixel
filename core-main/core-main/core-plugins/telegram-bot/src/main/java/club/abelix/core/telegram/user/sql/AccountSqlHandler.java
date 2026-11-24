package club.abelix.core.telegram.user.sql;

import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.NonNull;
import lombok.val;

import java.util.concurrent.TimeUnit;

public class AccountSqlHandler {

    public static final AccountSqlHandler INSTANCE = new AccountSqlHandler();

    /**
     * Запрос на загрузку информации о привязанных аккаунтах по VK
     */
    private static final String GET_PLAYER_PASSWORD = "SELECT `password` FROM `NewAuth` WHERE `Name` = ?";

    /**
     * Запрос на загрузку информации о привязанных аккаунтах по VK
     */
    private static final String IS_PREMIUM_AUTH = "SELECT `premium_auth` FROM `NewAuth` WHERE `Name` = ?";

    /**
     * Запрос на изменение пароля игрока
     */
    private static final String CHANGE_PLAYER_PASSWORD = "UPDATE `NewAuth` SET `password` = ? WHERE `Name` = ?";

    private final Cache<String, Account> accountCache = Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .build();

    private final Database database = new MySQL(DatabaseCredentials.MAIN.withDatabase("playerdata"));

    /**
     * Получить аккаунт игрока
     *
     * @param playerName - ник игрока
     * @return - аккаунт игрока
     */
    public Account getAccount(final @NonNull String playerName) {
        return accountCache.get(playerName.toLowerCase(), key -> {
            val password = getPlayerPassword(playerName);

            if (password == null) {
                return new Account(playerName, null, false);
            }

            val premiumAuth = isPremiumAuth(playerName);

            return new Account(playerName, password, premiumAuth);
        });
    }

    /**
     * Получить пароль игрока по его нику
     *
     * @param playerName - ник игрока
     * @return - пароль игрока (хеш пароля)
     * null, если аккаунт не будет зареган
     */
    private String getPlayerPassword(final @NonNull String playerName) {
        return database.executeQuery(GET_PLAYER_PASSWORD, playerName.toLowerCase())
                .thenApply(rows -> !rows.isEmpty()
                        ? rows.getFirst().getRequiredString("password")
                        : null
                ).join();
    }

    /**
     * Изменить пароль игрока
     *
     * @param playerName   - ник игрока
     * @param passwordHash - хеш нового пароля
     */
    public void changePlayerPassword(
            final @NonNull String playerName,
            final @NonNull String passwordHash
    ) {
        database.executeVoidUpdate(CHANGE_PLAYER_PASSWORD, passwordHash, playerName.toLowerCase());
        accountCache.invalidate(playerName.toLowerCase());
    }

    /**
     * Проверить, является ли аккаунт премиумным
     *
     * @param playerName - ник игрока
     * @return - true, если аккаунт премиумный
     */
    private boolean isPremiumAuth(final @NonNull String playerName) {
        return database.executeQuery(IS_PREMIUM_AUTH, playerName.toLowerCase())
                .thenApply(rows -> !rows.isEmpty() && rows.getFirst().getBoolean("premium_auth"))
                .join();
    }

    public record Account(String name, String password, boolean premiumAuth) {
        public boolean registered() {
            return password != null;
        }
    }
}
