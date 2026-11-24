package club.abelix.commons.data.skin;

import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import lombok.NonNull;
import lombok.val;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SkinsSqlHandler {

    public static final SkinsSqlHandler INSTANCE = new SkinsSqlHandler();

    /**
     * Запрос на получении истории скинов игрока
     */
    private static final String GET_SKIN_HISTORY = "SELECT `Skin`, `Time` FROM `SkinsHistory` WHERE `Id` = ? ORDER BY `Time` DESC;";

    /**
     * Запрос на добавление скина в историю
     */
    private static final String ADD_SKIN_HISTORY = "INSERT INTO `SkinsHistory` (`Id`, `Skin`, `Time`) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `Time` = ?";

    /**
     * Удаляем все "лишние" скины из истории скинов (там же максимум N записей)
     */
    private static final String TRIM_SKIN_HISTORY = "DELETE FROM `SkinsHistory` WHERE `Id` = ? AND `Skin` IN (SELECT `Skin` FROM (SELECT `Skin` FROM ` skins`.`SkinsHistory` WHERE `Id` = ? ORDER BY `Time` DESC LIMIT 1 OFFSET ?) AS `Skin`);";

    /**
     * Запрос на получение установленного скина игрока
     */
    private static final String GET_CUSTOM_SKIN = "SELECT `Skin` FROM `PlayerSkins` WHERE `Id` = ?;";

    /**
     * Изменить установленный скин игрока
     */
    private static final String SET_CUSTOM_SKIN = "INSERT INTO `PlayerSkins` (`Id`, `Skin`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `Skin` = ?;";

    /**
     * Удалить установленный скин игрока
     */
    private static final String REMOVE_CUSTOM_SKIN = "DELETE FROM `PlayerSkins` WHERE `Id` = ?";

    /**
     * Получить данные о сохраненном в базе скине
     */
    private static final String GET_SKIN_DATA = "SELECT * FROM `SkinsStorage` WHERE `Name` = ?;";

    /**
     * Добавить в базу данные о новом скине
     */
    private static final String ADD_SKIN_DATA = "INSERT INTO `SkinsStorage` (`Name`, `Uuid`, `Value`, `Signature`, `Timestamp`) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE `Uuid` = ?, `Value`= ?, `Signature`= ?, `Timestamp` = ?;";

    /**
     * Получить данные об N рандомных скинов из БД
     */
    private static final String GET_RANDOM_SKINS_DATA = "SELECT * FROM `SkinsStorage` ORDER BY RAND() LIMIT ?";

    private final Database database = new MySQL(DatabaseCredentials.MAIN.withDatabase("skins"));

    /**
     * Получить историю скинов по id игрока
     *
     * @param playerId - id игрока
     * @return - история скинов игрока
     */
    // юзаем тут не TObjectLongMap, а обычную, потому что trove не дает нам порядка в хешмапе...
    public LinkedHashMap<String, LocalDateTime> getSkinHistory(final int playerId) {
        return database.executeQuery(GET_SKIN_HISTORY, playerId).thenApply(rows -> {
            val skins = new LinkedHashMap<String, LocalDateTime>();

            for (val row : rows) {
                val skin = row.getRequiredString("Skin");
                val time = row.getRequiredTimestamp("Time");

                skins.put(skin, time);
            }

            return skins;
        }).join();
    }

    /**
     * Добавить скин в историю скинов игрока
     *
     * @param playerId - id игрока
     * @param skin     - имя скина
     * @param time     - время последней установки скина
     */
    public void addSkinHistory(
            final int playerId,
            final @NonNull String skin,
            final @NonNull LocalDateTime time
    ) {
        database.executeVoidUpdate(ADD_SKIN_HISTORY, playerId, skin.toLowerCase(), time, time);
    }

    /**
     * Удаление лишних записей из истории скинов
     *
     * @param playerId - id игрока
     * @param limit    - лимит количества скинов
     */
    public void trimSkinHistory(final int playerId, final int limit) {
        database.executeVoidUpdate(TRIM_SKIN_HISTORY, playerId, playerId, limit);
    }

    // endregion

    // region Custom Skin

    /**
     * Получить установленный игроком скин
     *
     * @param playerId - id игрока
     * @return - установленный скин игрока
     */
    public CompletableFuture<String> getCustomSkin(final int playerId) {
        return database.executeQuery(GET_CUSTOM_SKIN, playerId)
                .thenApply(rows -> !rows.isEmpty()
                        ? rows.getFirst().getRequiredString("Skin")
                        : null
                );
    }

    /**
     * Установить кастомный скин игроку
     *
     * @param playerId - id игрока
     * @param skin     - имя скина
     */
    public void setCustomSkin(final int playerId, final @NonNull String skin) {
        val skinName = skin.toLowerCase();

        database.executeVoidUpdate(SET_CUSTOM_SKIN, playerId, skinName, skinName);
    }

    /**
     * Удалить выбранный скин игрока
     *
     * @param playerId - id игрока
     */
    public void removeCustomSkin(final int playerId) {
        database.executeVoidUpdate(REMOVE_CUSTOM_SKIN, playerId);
    }

    // endregion

    // region Skin

    /**
     * Получить данные кешированного скина (value, signature)
     *
     * @param name - имя скина
     * @return - данные скина, или null, если данных нет
     */
    public Skin getSkinByName(final @NonNull String name) {
        return database.executeQuery(GET_SKIN_DATA, name).thenApply(rows -> {
            if (!rows.isEmpty()) {
                val row = rows.getFirst();

                return new Skin(name,
                        row.getRequiredString("Uuid"),
                        row.getRequiredString("Value"),
                        row.getRequiredString("Signature"),
                        row.getRequiredTimestamp("Timestamp")
                );
            }

            return null;
        }).join();
    }

    /**
     * Добавить данные о новом скине в базу данных,
     * либо обновить данные о существующем
     *
     * @param skin - данные о скине
     */
    public void addSkin(final @NonNull Skin skin) {
        database.executeVoidUpdate(ADD_SKIN_DATA,
                skin.getName(), skin.getUuid(), skin.getValue(), skin.getSignature(), skin.getTimestamp(),
                skin.getUuid(), skin.getValue(), skin.getSignature(), skin.getTimestamp());
    }

    // endregion

    /**
     * Получить данные об N рандомных скинах из базы данных
     *
     * @param amount - максимальное количество скинов
     * @return - данные о скинах
     */
    public List<Skin> getRandomSkins(final int amount) {
        return database.executeQuery(GET_RANDOM_SKINS_DATA, amount)
                .thenApply(rows -> {
                    val skins = new ArrayList<Skin>(amount);

                    for (val row : rows) {
                        skins.add(
                                new Skin(
                                        row.getRequiredString("Name"),
                                        row.getRequiredString("Uuid"),
                                        row.getRequiredString("Value"),
                                        row.getRequiredString("Signature"),
                                        row.getRequiredTimestamp("Timestamp")
                                )
                        );
                    }

                    return skins;
                })
                .join();
    }

}
