package club.abelix.commons.data.property;

import club.abelix.commons.data.punishment.BansSqlHandler;
import club.abelix.commons.mysql.modern.BufferedRow;
import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import lombok.NonNull;

import java.util.concurrent.CompletableFuture;

public class PropertySqlHandler {

    /*
        1_STAR_CASE
        2_STAR_CASE
        3_STAR_CASE

        CASE_DUST

        SW_KIT_SELECT 23


        CLOUDS
        XP (Level)
     */

    public static final PropertySqlHandler INSTANCE = new PropertySqlHandler();
    /**
     * Узнать значение проперки у игрока
     */
    public static final String GET_PROPERTY_VALUE = "SELECT `Value` FROM `playerdata`.`Properties` WHERE `Id` = ? AND `Property` = ?";
    /**
     * Загрузить проперти игрока
     */
    private static final String LOAD_PROPERTIES = "SELECT `Property`, `Value` FROM `playerdata`.`Properties` WHERE `Id` = ?;";
    /**
     * Добавить значение проперти у игрока
     */
    private static final String ADD_PROPERY_VALUE = "INSERT INTO `Properties` (`Id`, `Property`, `Value`) VALUES (?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE `Value` = `Value` + ?;";

    /**
     * Обновить значение проперти у игрока
     */
    private static final String SET_PROPERTY_VALUE = "INSERT INTO `Properties` (`Id`, `Property`, `Value`) VALUES (?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE `Value` = ?;";

    /**
     * Удалить проперти у игрока
     */
    private static final String REMOVE_PROPERTY = "DELETE FROM `Properties` WHERE `Id` = ? AND `Property` = ?;";

    /**
     * Загрузить проперти N лучших игроков из базы данных
     */
    private static final String LOAD_TOP_PLAYER_PROPERTIES = "SELECT `Id`, `Value` FROM `Properties` WHERE `Property` = ? ORDER BY `Value` %s;";

    private final Database database = new MySQL(DatabaseCredentials.MAIN.withDatabase("playerdata"));

    public CompletableFuture<TObjectIntMap<String>> loadProperties(int playerId) {
        return database.executeQuery(LOAD_PROPERTIES, playerId).thenApply(rows -> {
            TObjectIntMap<String> properties = new TObjectIntHashMap<>();

            for (BufferedRow row : rows) {
                properties.put(
                        row.getRequiredString("Property").toLowerCase(),
                        row.getRequiredInt("Value")
                );
            }

            return properties;
        });
    }

    public CompletableFuture<Integer> getPropertyValue(int playerId, @NonNull String property) {
        return database.executeQuery(GET_PROPERTY_VALUE, playerId, property).thenApply(rows -> {
            if (!rows.isEmpty()) {
                return rows.getFirst().getRequiredInt("Value");
            }

            return 0;
        });
    }

    public CompletableFuture<?> addPropertyValue(int playerId, @NonNull String property, int value) {
        return database.executeVoidUpdate(ADD_PROPERY_VALUE, playerId, property, value, value);
    }

    public CompletableFuture<?> setPropertyValue(int playerId, @NonNull String property, int value) {
        return database.executeVoidUpdate(SET_PROPERTY_VALUE, playerId, property, value, value);
    }

    public CompletableFuture<?> removeProperty(int playerId, @NonNull String property) {
        return database.executeVoidUpdate(REMOVE_PROPERTY, playerId, property);
    }

    public CompletableFuture<TIntIntMap> loadTopPlayerProperties(
            @NonNull String property, boolean ascending, int limit) {

        return database.executeQuery(String.format(LOAD_TOP_PLAYER_PROPERTIES, ascending ? "ASC" : "DESC"), property)
                .thenApply(rows -> {
                    TIntIntMap topPlayers = new TIntIntHashMap(limit);

                    for (BufferedRow row : rows) {
                        int playerId = row.getRequiredInt("Id");

                        if (BansSqlHandler.INSTANCE.isBanned(playerId).join()) {
                            continue;
                        }

                        topPlayers.put(playerId, row.getRequiredInt("Value"));

                        if (topPlayers.size() == limit) {
                            break;
                        }
                    }

                    return topPlayers;
                });
    }

}
