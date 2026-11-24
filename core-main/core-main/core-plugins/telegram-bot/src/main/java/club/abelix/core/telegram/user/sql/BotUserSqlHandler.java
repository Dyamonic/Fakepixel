package club.abelix.core.telegram.user.sql;

import club.abelix.commons.language.Language;
import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import club.abelix.core.telegram.user.BotUser;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import lombok.NonNull;
import lombok.val;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Репозиторий бота (тут вся инфа о его пользователях)
 */
public class BotUserSqlHandler {

    public static final BotUserSqlHandler INSTANCE = new BotUserSqlHandler();

    /**
     * Мапа пользователей бота
     */
    private final Cache<Long, BotUser> userCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    /**
     * Получить пользователя бота по ID в Telegram
     *
     * @param telegramId - id в Telegram
     * @return - пользователь бота
     */
    public BotUser getBotUser(final long telegramId) {
        userCache.cleanUp();

        return userCache.get(telegramId, key -> loadBotUser(telegramId));
    }

    /**
     * Получить пользователя бота по нику аккаунта
     *
     * @param playerName - ник в игре
     * @return - пользователь бота
     */
    public BotUser getBotUser(@NonNull final String playerName) {
        userCache.cleanUp();

        //сначала ищем в мапе уже загруженных юзеров
        for (BotUser botUser : userCache.asMap().values()) {
            if (botUser.hasLinkedAccount(playerName)) {
                return botUser;
            }
        }

        return loadBotUser(playerName);
    }

    /**
     * Удалить пользователя из кеша
     *
     * @param userId - user_id
     */
    public void invalidateUserFromCache(final long userId) {
        userCache.invalidate(userId);
    }

    /**
     * Запрос на загрузку информации о привязанных аккаунтах по нику
     */
    private static final String LOAD_BOT_ACCOUNT_BY_TELEGRAM = "SELECT `Name` FROM `NewAuth` WHERE `telegram` = ?";

    /**
     * Запрос на загрузку информации о привязанных аккаунтах по Telegram
     */
    private static final String LOAD_BOT_ACCOUNT_BY_NAME = "SELECT `telegram` FROM `NewAuth` WHERE `Name` = ?";

    /**
     * Запрос на загрузку всех аккаунтов бота
     */
    private static final String LOAD_ALL_BOT_ACCOUNTS = "SELECT * FROM `NewAuth` WHERE `telegram` > 0";

    /**
     * Установка языка пользователя
     */
    private static final String SET_USER_LANGUAGE = "INSERT INTO `BotLanguage` (`Telegram`, `Language`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `Language` = ?";

    /**
     * Установка языка пользователя
     */
    private static final String LOAD_USER_LANGUAGE = "SELECT `Language` FROM `BotLanguage` WHERE `Telegram` = ?";

    /**
     * Запрос на добавление привязки ника к боту
     */
    private static final String CHANGE_BOT_LINKAGE = "UPDATE `NewAuth` SET `telegram` = ? WHERE `Name` = ?";

    /**
     * Запрос на получение главного аккаунта
     */
    private static final String GET_PRIMARY_ACCOUNT = "SELECT `Name` FROM `BotAccounts` WHERE `Id` = ?";

    /**
     * Запрос на обновление главного аккаунта
     */
    private static final String SET_PRIMARY_ACCOUNT = "INSERT INTO `BotAccounts` (`Id`, `Name`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `Name` = ?";

    /**
     * Запрос на удаление записи об основном аккаунте
     */
    private static final String REMOVE_PRIMARY_ACCOUNT = "DELETE FROM `BotAccounts` WHERE `Id` = ?";

    /**
     * Загрузить проперти аккаунта
     */
    private static final String LOAD_PROPERTIES = "SELECT `Property`, `Value` FROM `BotProperties` WHERE `Id` = ?;";

    /**
     * Узнать значение проперти аккаунта
     */
    public static final String GET_PROPERTY_VALUE = "SELECT `Value` FROM `BotProperties` WHERE `Id` = ? AND `Property` = ?";

    /**
     * Добавить значение проперти аккаунта
     */
    private static final String ADD_PROPERTY_VALUE = "INSERT INTO `BotProperties` (`Id`, `Property`, `Value`) VALUES (?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE `Value` = `Value` + ?;";

    /**
     * Обновить значение проперти аккаунта
     */
    private static final String SET_PROPERTY_VALUE = "INSERT INTO `BotProperties` (`Id`, `Property`, `Value`) VALUES (?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE `Value` = ?;";

    /**
     * Удалить проперти аккаунта
     */
    private static final String REMOVE_PROPERTY = "DELETE FROM `BotProperties` WHERE `Id` = ? AND `Property` = ?;";


    // *********************************************** //

    private final Database botDatabase = new MySQL(DatabaseCredentials.MAIN.withDatabase("TelegramBot"));
    private final Database playerDatabase = new MySQL(DatabaseCredentials.MAIN.withDatabase("playerdata"));

    /**
     * Загрузка пользователя бота по ID в Telegram
     */
    public BotUser loadBotUser(final long telegramId) {
        val linkedAccounts = playerDatabase.executeQuery(LOAD_BOT_ACCOUNT_BY_TELEGRAM, telegramId)
                .thenApply(rows ->
                        rows.stream()
                                .map(row -> row.getRequiredString("Name").toLowerCase())
                                .collect(Collectors.toList()))
                .join();

        val primaryAccount = botDatabase.executeQuery(GET_PRIMARY_ACCOUNT, telegramId)
                .thenApply(rows -> !rows.isEmpty()
                        ? rows.getFirst().getRequiredString("Name").toLowerCase()
                        : null
                )
                .join();

        val language = botDatabase.executeQuery(LOAD_USER_LANGUAGE, telegramId)
                .thenApply(rows -> rows.isEmpty() ? null
                        : Language.getById(rows.getFirst().getRequiredInt("Language")))
                .join();

        val properties = loadProperties(telegramId).join();

        return new BotUser(telegramId, linkedAccounts, primaryAccount, language, properties);
    }

    public CompletableFuture<TObjectIntMap<String>> loadProperties(long botId) {
        return botDatabase.executeQuery(LOAD_PROPERTIES, botId).thenApply(rows -> {
            val properties = new TObjectIntHashMap<String>();

            for (val row : rows) {
                properties.put(
                        row.getRequiredString("Property").toLowerCase(),
                        row.getRequiredInt("Value")
                );
            }

            return properties;
        });
    }

    public CompletableFuture<Integer> getPropertyValue(long botId, @NonNull String property) {
        return botDatabase.executeQuery(GET_PROPERTY_VALUE, botId, property).thenApply(rows -> {
            if (!rows.isEmpty()) {
                return rows.getFirst().getRequiredInt("Value");
            }

            return 0;
        });
    }

    public CompletableFuture<List<Long>> loadAllBotAccounts() {
        return playerDatabase.executeQuery(LOAD_ALL_BOT_ACCOUNTS).thenApply(rows -> rows.stream()
                .map(row -> row.getRequiredLong("telegram"))
                .collect(Collectors.toList()));
    }

    public CompletableFuture<?> addPropertyValue(long botId, @NonNull String property, int value) {
        return botDatabase.executeVoidUpdate(ADD_PROPERTY_VALUE, botId, property, value, value);
    }

    public CompletableFuture<?> setPropertyValue(long botId, @NonNull String property, int value) {
        return botDatabase.executeVoidUpdate(SET_PROPERTY_VALUE, botId, property, value, value);
    }

    public CompletableFuture<?> removeProperty(long botId, @NonNull String property) {
        return botDatabase.executeVoidUpdate(REMOVE_PROPERTY, botId, property);
    }

    /**
     * Загрузка пользователя бота по нику
     */
    public BotUser loadBotUser(@NonNull final String playerName) {
        return playerDatabase.executeQuery(LOAD_BOT_ACCOUNT_BY_NAME, playerName)
                .thenApply(rows -> {
                    val telegramId = !rows.isEmpty()
                            ? rows.getFirst().getLong("telegram").orElse(0)
                            : 0;

                    return telegramId <= 0 ? BotUser.EMPTY_BOT_USER : getBotUser(telegramId);
                })
                .join();
    }

    /**
     * Привязать аккаунт к Telegram
     *
     * @param botUser     - пользователь TG-бота
     * @param accountName - ник игрока
     */
    public void linkBotAccount(@NonNull final BotUser botUser, @NonNull final String accountName) {
        botUser.addLinkedAccount(accountName);

        playerDatabase.executeVoidUpdate(CHANGE_BOT_LINKAGE, botUser.getTelegramId(), accountName);
    }

    /**
     * Отвязать аккаунт от Telegram
     *
     * @param botUser     - пользователь TG-бота
     * @param accountName - ник игрока
     */
    public void unLinkBotAccount(@NonNull final BotUser botUser, @NonNull final String accountName) {
        val isPrimaryAccount = botUser.hasPrimaryAccount() &&
                botUser.getPrimaryAccountName().equalsIgnoreCase(accountName);

        botUser.removeLinkedAccount(accountName);

        if (isPrimaryAccount) {
            botDatabase.executeVoidUpdate(REMOVE_PRIMARY_ACCOUNT, botUser.getTelegramId());
        }

        playerDatabase.executeVoidUpdate(CHANGE_BOT_LINKAGE, 0, accountName);
    }

    /**
     * Сделать аккаунт основным (primary)
     *
     * @param botUser     - пользователь бота
     * @param accountName - ник аккаунта
     */
    public void linkPrimaryAccount(@NonNull final BotUser botUser, @NonNull String accountName) {
        accountName = accountName.toLowerCase();

        botUser.setPrimaryAccountName(accountName);

        botDatabase.executeVoidUpdate(SET_PRIMARY_ACCOUNT, botUser.getTelegramId(), accountName, accountName);
    }

    public void setLanguage(@NonNull final BotUser botUser, @NonNull final Language language) {
        botUser.setLanguage(language);

        botDatabase.executeVoidUpdate(SET_USER_LANGUAGE, botUser.getTelegramId(), language.getId(), language.getId());
    }

}
