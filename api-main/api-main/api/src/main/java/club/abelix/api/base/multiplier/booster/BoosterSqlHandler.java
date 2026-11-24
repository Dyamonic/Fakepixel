package club.abelix.api.base.multiplier.booster;

import club.abelix.commons.data.booster.BoosterType;
import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import lombok.NonNull;
import lombok.val;

public class BoosterSqlHandler {

    public static final BoosterSqlHandler INSTANCE = new BoosterSqlHandler();

    /**
     * Запрос на получение текущего бустера игрока
     */
    private static final String LOAD_BOOSTER = "SELECT `BoosterLevel`, `Expire` FROM `playerdata`.`Boosters` WHERE `Id` = ?;";

    /**
     * Запрос на изменение текущего бустера игрока
     */
    private static final String SET_BOOSTER = "INSERT INTO `playerdata`.`Boosters` (`Id`, `BoosterLevel`, `Expire`) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `BoosterLevel` = ?, `Expire` = ?;";

    /**
     * Запрос на удаление текущего бустера у игрока
     */
    private static final String REMOVE_BOOSTER = "DELETE FROM `playerdata`.`Boosters` WHERE `Id` = ?;";

    private final Database database = new MySQL(DatabaseCredentials.MAIN.withDatabase("playerdata"));

    public Booster loadBooster(int playerId) {
        return database.executeQuery(LOAD_BOOSTER, playerId).thenApply(rows -> {
            Booster booster = null;

            if (!rows.isEmpty()) {
                val row = rows.getFirst();

                val boosterType = BoosterType
                        .getById(row.getRequiredInt("BoosterLevel"));

                if (boosterType == null) {
                    return null;
                }

                booster = new Booster(boosterType, row.getRequiredTimestamp("Expire"));

                // если срок действия бустера истек то вернем null
                if (booster.isExpired()) {
                    removeBooster(playerId);
                    booster = null;
                }
            }

            return booster;
        }).join();
    }

    public void setBooster(int playerId, @NonNull Booster multiplier) {
        val boosterType = multiplier.getBoosterType().getId();
        val expires = multiplier.getExpire();

        database.executeVoidUpdate(SET_BOOSTER,
                playerId, boosterType, expires, boosterType, expires);
    }

    public void removeBooster(int playerId) {
        database.executeVoidUpdate(REMOVE_BOOSTER, playerId);
    }

}
