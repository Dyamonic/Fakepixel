package club.abelix.commons.data.network;

import club.abelix.commons.language.Language;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.NonNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class NetworkSqlHandler {

    public static final NetworkSqlHandler INSTANCE = new NetworkSqlHandler();

    /**
     * Запрос на получение ID игрока
     */
    private static final String LOAD_PLAYER_ID = "SELECT `Id` FROM `playerdata`.`NewAuth` WHERE `Name` = ?;";

    /**
     * Запрос на получение имени игрока
     */
    private static final String LOAD_PLAYER_NAME = "SELECT `Name` FROM `NewAuth` WHERE `Id` = ?;";

    /**
     * Запрос на обновление ника игрока
     */
    private static final String CHANGE_PLAYER_NAME = "UPDATE `NewAuth` SET `Name` = ? WHERE `Name` = ?";

    /**
     * Запрос на получение языка игрока
     */
    private static final String GET_PLAYER_LANGUAGE = "SELECT `language` FROM `NewAuth` WHERE `Id` = ?";

    /**
     * Запрос на обновление языка игрока
     */
    private static final String CHANGE_PLAYER_LANGUAGE = "UPDATE `NewAuth` SET `language` = ? WHERE `Id` = ?";

    /**
     * Количество записей в таблице
     */
    private static final String GET_TOTAL_RECORDS = "SELECT COUNT(*) AS total_records FROM `NewAuth`;";

    private final MySQL database = new MySQL(DatabaseCredentials.MAIN.withDatabase("playerdata"));

    /**
     * Кеш ID игроков, нужен чтобы не делать много лишних запросов
     * в базу после очередного получения ID игрока
     */
    private final Cache<String, Integer> playerIdCache = Caffeine.newBuilder()
            .expireAfterWrite(6, TimeUnit.HOURS)
            .build();

    /**
     * Получить ник игрока по id. Получает ник из БД,
     * а затем кеширует информацию на час, чтобы избежать
     * лишних запросов в базу данных. По умолчанию юзается
     * с возможностью кеширования
     *
     * @param playerName - ник игрока
     * @return - id игрока
     */
    public int getPlayerId(@NonNull String playerName) {
        return getPlayerId(playerName, true);
    }

    /**
     * Получить id игрока по нику. Получает из БД ID,
     * а затем кеширует его на час, чтобы избежать лишних
     * запросов в базу данных
     *
     * @param playerName - ник игрока
     * @param useCache   - использовать ли кеш
     * @return - id игрока
     */
    public int getPlayerId(@NonNull String playerName, boolean useCache) {
        playerIdCache.cleanUp();

        if (useCache) {
            Map<String, Integer> cacheAsMap = playerIdCache.asMap();

            //чекаем если в кеше уже есть этот игрок
            //просто по ключу получить не можем - кэш не case insensitive
            Map.Entry<String, Integer> cachedEntry
                    = cacheAsMap.entrySet().stream().filter(entry -> entry.getKey().equalsIgnoreCase(playerName)).findFirst().orElse(null);

            if (cachedEntry != null) {
                return cachedEntry.getValue();
            }
        }

        return database.executeQuery(LOAD_PLAYER_ID, playerName).thenApply(rows -> {
            if (rows.isEmpty()) {
                return -1;
            }

            int id = rows.getFirst().getRequiredInt("Id");

            if (useCache) {
                playerIdCache.put(playerName, id);
            }

            return id;
        }).join();
    }

    /**
     * Получить ник игрока по id. Получает ник из БД,
     * а затем кеширует информацию на час, чтобы избежать
     * лишних запросов в базу данных. По умолчанию юзается
     * с возможностью кеширования
     *
     * @param playerId - id игрока
     * @return - ник игрока
     */
    public String getPlayerName(int playerId) {
        return getPlayerName(playerId, true);
    }

    /**
     * Получить ник игрока по id. Получает ник из БД,
     * а затем кеширует информацию на час, чтобы избежать
     * лишних запросов в базу данных
     *
     * @param playerId - id игрока
     * @param useCache - использовать ли кеш и кешировать ли данные
     * @return - ник игрока
     */
    public String getPlayerName(int playerId, boolean useCache) {
        playerIdCache.cleanUp();

        if (useCache) {
            Map<String, Integer> cacheAsMap = playerIdCache.asMap();

            //чекаем если в кеше уже есть этот игрок
            if (cacheAsMap.containsValue(playerId)) {
                Map.Entry<String, Integer> cachedEntry
                        = cacheAsMap.entrySet().stream().filter(entry -> entry.getValue() == playerId).findFirst().orElse(null);

                if (cachedEntry != null) {
                    return cachedEntry.getKey();
                }
            }
        }

        return database.executeQuery(LOAD_PLAYER_NAME, playerId).thenApply(rows -> {
            if (rows.isEmpty()) {
                return null;
            }

            String name = rows.getFirst().getRequiredString("Name");

            if (useCache) {
                playerIdCache.put(name, playerId);
            }

            return name;
        }).join();
    }

    /**
     * Добавить ID игрока в кеш
     *
     * @param playerName - ник игрока
     * @param playerId   - id игрока
     */
    public void cachePlayerId(@NonNull String playerName, int playerId) {
        playerIdCache.cleanUp();
        playerIdCache.put(playerName, playerId);
    }

    /**
     * Удалить id игрока из кэша
     *
     * @param playerName - ник игрока
     */
    public void removeFromCache(@NonNull String playerName) {
        playerIdCache.cleanUp();
        playerIdCache.invalidate(playerName);
    }

    /**
     * Поменять местами данные об аккаунтах
     *
     * @param name       - ник первого аккаунта
     * @param targetName - ник второго аккаунта
     */
    public void migrateAccount(@NonNull String name, @NonNull String targetName) {
        playerIdCache.invalidate(name);
        playerIdCache.invalidate(targetName);

        //чтобы если какой-нибудь еще индивидум залезет или зарегает определенный ник, не произошел пиздец
        String temporaryAccountName = UUID.randomUUID().toString().substring(0, 16);

        database.executeVoidUpdate(CHANGE_PLAYER_NAME, temporaryAccountName, targetName).join();
        database.executeVoidUpdate(CHANGE_PLAYER_NAME, targetName, name).join();
        database.executeVoidUpdate(CHANGE_PLAYER_NAME, name, temporaryAccountName).join();
    }

    /**
     * Изменить язык игрока
     *
     * @param playerId - id игрока
     * @param language - язык игрока
     */
    public void setPlayerLanguage(int playerId, @NonNull Language language) {
        database.executeVoidUpdate(CHANGE_PLAYER_LANGUAGE, language.getId(), playerId);
    }

    /**
     * Узнать статус язык игрока
     *
     * @param playerId - id игрока
     * @return - язык игрока
     */
    @Deprecated
    public Language getPlayerLanguage(int playerId) {
        return _getPlayerLanguage(playerId).join();
    }

    public CompletableFuture<Integer> getTotalRegisteredPlayers() {
        return database.executeQuery(GET_TOTAL_RECORDS).thenApply(rows -> {
            if (rows.isEmpty()) {
                return 0;
            }

            return rows.getFirst().getRequiredInt("total_records");
        });
    }

    public CompletableFuture<Language> _getPlayerLanguage(int playerId) {
        return database.executeQuery(GET_PLAYER_LANGUAGE, playerId).thenApply(rows -> {
            if (rows.isEmpty()) {
                return Language.defaultLanguage();
            }

            return Language.getById(rows.getFirst().getRequiredInt("language"));
        });
    }
}
