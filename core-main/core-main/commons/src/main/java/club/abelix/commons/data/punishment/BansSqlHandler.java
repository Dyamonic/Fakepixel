package club.abelix.commons.data.punishment;

import club.abelix.commons.data.property.PropertySqlHandler;
import club.abelix.commons.data.punishment.meta.BanMeta;
import club.abelix.commons.data.punishment.meta.PunishmentMeta;
import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import lombok.NonNull;
import lombok.val;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class BansSqlHandler {

    public static final BansSqlHandler INSTANCE = new BansSqlHandler();

    //-------------------------------------------------------------//
    //                      Для системы банов                      //
    //-------------------------------------------------------------//

    /**
     * Запрос на загрузку всех BanMeta
     */
    private static final String LOAD_BAN_METAS_JOINED = """
                SELECT
                    bm.Id,
                    bm.Expire,
                    bm.Time,
                    bm.Admin,
                    bm.Removed,
                    bm.Reason,
                    GROUP_CONCAT(DISTINCT bn.Name SEPARATOR ',') AS BanNames,
                    GROUP_CONCAT(DISTINCT bi.Address SEPARATOR ',') AS BanAddresses
                FROM
                    BansMeta bm
                LEFT JOIN
                    BansName bn ON bm.Id = bn.Id
                LEFT JOIN
                    BansIP bi ON bm.Id = bi.Id
                GROUP BY
                    bm.Id
            """;

    /**
     * Запрос на добавление новой BanMeta
     */
    private static final String ADD_BAN_META = "INSERT INTO `BansMeta` (`Admin`, `Time`, `Expire`, `Reason`) VALUES (?, ?, ?, ?)";

    /**
     * Запрос на переход BanMeta из активного в неактивное состояние
     * (разбан), с указанием ника разбанившего игрока и времени разбана
     */
    private static final String FINISH_BAN_META = "UPDATE `BansMeta` SET `Expire` = ?, `Removed` = ? WHERE `Id` = ?";

    /**
     * Запрос на полное удаление бана из истории
     */
    private static final String REMOVE_BAN_META = "DELETE FROM `BansMeta` WHERE `Id` = ?";

    /**
     * Добавление бана по имени для указанного ID бана
     */
    private static final String ADD_BAN_BY_NAME = "INSERT IGNORE INTO `BansName` (`Id`, `Name`) VALUES (?, ?)";

    /**
     * Добавление бана по IP для указанного ID бана
     */
    private static final String ADD_BAN_BY_IP = "INSERT IGNORE INTO `BansIP` (`Id`, `Address`) VALUES (?, ?)";

    /**
     * Получить баны по имени для указанного ID бана
     */
    private static final String GET_NAME_BANS = "SELECT `Name` FROM `BansName` WHERE `Id` = ?";

    /**
     * Получить базы по IP для указанного ID бана
     */
    private static final String GET_IP_BANS = "SELECT `Address` FROM `BansIP` WHERE `Id` = ?";

    private static final String GET_ACTIVE_BANS_COUNT = "SELECT COUNT(*) AS `count` FROM `BansMeta` WHERE `Removed` IS NULL";

    private static final String GET_BANS_FROM_DATE = "SELECT COUNT(*) AS `count` FROM `BansMeta` WHERE `Time` >= ?";


    private final Database database = new MySQL(DatabaseCredentials.MAIN.withDatabase("newbans"));

    /**
     * Загрузка списка всех банов из базы данных
     *
     * @return - список банов
     */
    public CompletableFuture<List<BanMeta>> loadBanList() {
        return database.executeQuery(LOAD_BAN_METAS_JOINED).thenApply(result -> {
            val banMetaList = new ArrayList<BanMeta>();

            for (val row : result) {
                val banId = row.getRequiredInt("Id");

                val expireTime = row.getTimestamp("Expire")
                        .map(time -> time.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000)
                        .orElse(PunishmentMeta.PERMANENT_TIME);

                val banTime = row.getRequiredTimestamp("Time")
                        .atZone(ZoneId.systemDefault()).toEpochSecond() * 1000;

                val adminName = row.getRequiredString("Admin");
                val unBanBy = row.getString("Removed").orElse(null);
                val reason = row.getRequiredString("Reason");

                val banMeta = new BanMeta(banId, adminName, unBanBy, banTime, expireTime, reason);

                row.getString("BanNames").ifPresent(names ->
                        Arrays.stream(names.split(",")).forEach(banMeta::addName));
                row.getString("BanAddresses").ifPresent(addresses ->
                        Arrays.stream(addresses.split(",")).forEach(banMeta::addAddress));

                banMetaList.add(banMeta);
            }

            return banMetaList;
        });
    }

    /**
     * Получить список банов по нику для указанного ID бана
     *
     * @param banId - id бана
     * @return - список банов по нику
     */
    private CompletableFuture<List<String>> getNameBans(int banId) {
        return database.executeQuery(GET_NAME_BANS, banId)
                .thenApply(query -> query.stream()
                        .map(row -> row.getRequiredString("Name"))
                        .collect(Collectors.toList()));
    }

    /**
     * Получить список банов по нику для указанного ID бана
     *
     * @param banId - id бана
     * @return - список банов по нику
     */
    private CompletableFuture<List<String>> getIpBans(int banId) {
        return database.executeQuery(GET_IP_BANS, banId)
                .thenApply(query -> query.stream()
                        .map(row -> row.getRequiredString("Address"))
                        .collect(Collectors.toList()));
    }

    /**
     * Добавить информацию о новом бане в базу данных и получить его ID
     *
     * @param admin  - ник администратора, выдавшего бан
     * @param time   - время выдачи бана
     * @param expire - время истечения бана
     * @param reason - причина бана
     * @return - информация о бане с его уникальным номером
     */
    public CompletableFuture<BanMeta> addBanMeta(@NonNull String admin, long time, long expire, @NonNull String reason) {
        return database.executeUpdate(ADD_BAN_META, admin, new Timestamp(time),
                        expire != PunishmentMeta.PERMANENT_TIME ? new Timestamp(expire) : null, reason)
                .thenApply(query -> new BanMeta(query.getGeneratedKey(), admin, time, expire, reason));
    }

    /**
     * Проверить, есть ли бан у игрока
     *
     * @param playerId - id игрока
     * @return - true, если игрок забанен
     * false, если нет
     */
    public CompletableFuture<Boolean> isBanned(int playerId) {
        return PropertySqlHandler.INSTANCE.getPropertyValue(playerId, "Banned")
                .thenApply(value -> value == 1);
    }

    /**
     * Завершить активный бан досрочно администратором
     *
     * @param banId   - уникальный id бана
     * @param expire  - время завершения бана
     * @param unBanBy - ник администратора, который разбанил
     */
    public void finishBanMeta(int banId, long expire, @NonNull String unBanBy) {
        database.executeVoidUpdate(FINISH_BAN_META, new Timestamp(expire), unBanBy, banId);
    }

    /**
     * Полностью удалить информацию о блокировке из БД
     *
     * @param banId - id блокировки
     */
    public void removeBanMeta(int banId) {
        database.executeVoidUpdate(REMOVE_BAN_META, banId);
    }

    /**
     * Добавить блокировку по нику для указанного бана
     *
     * @param banId      - id бана
     * @param bannedName - ник игрока
     */
    public void addNameBan(int banId, @NonNull String bannedName) {
        database.executeVoidUpdate(ADD_BAN_BY_NAME, banId, bannedName);
    }

    /**
     * Добавить блокировку по ip для указанного бана
     *
     * @param banId   - id бана
     * @param address - ip адрес
     */
    public void addIpBan(int banId, @NonNull String address) {
        database.executeVoidUpdate(ADD_BAN_BY_IP, banId, address);
    }

    public CompletableFuture<Long> getActiveBansCount() {
        return database.executeQuery(GET_ACTIVE_BANS_COUNT)
                .thenApply(result -> result.getFirst().getRequiredLong("count"));
    }

    public CompletableFuture<Long> getBansFromDate(LocalDateTime time) {
        return getBansFromDate(time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }

    public CompletableFuture<Long> getBansFromDate(long dateMillis) {
        return database.executeQuery(GET_BANS_FROM_DATE, new Timestamp(dateMillis))
                .thenApply(result -> result.getFirst().getRequiredLong("count"));
    }
    // endregion

}
