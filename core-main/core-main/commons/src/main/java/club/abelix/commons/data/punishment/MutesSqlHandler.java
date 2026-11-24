package club.abelix.commons.data.punishment;

import club.abelix.commons.data.punishment.meta.MuteMeta;
import club.abelix.commons.data.punishment.meta.PunishmentMeta;
import club.abelix.commons.mysql.modern.BufferedRow;
import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import lombok.NonNull;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MutesSqlHandler {

    public static final MutesSqlHandler INSTANCE = new MutesSqlHandler();

    //-------------------------------------------------------------//
    //                      Для системы мутов                      //
    //-------------------------------------------------------------//

    /**
     * Запрос на загрузку всех MuteMeta
     */
    private static final String LOAD_MUTE_METAS = "SELECT * FROM `MutesMeta`;";

    /**
     * Запрос на добавление новой MuteMeta
     */
    private static final String ADD_MUTE_META = "INSERT INTO `MutesMeta` (`PlayerId`, `Admin`, `Time`, `Expire`, `Reason`) VALUES (?, ?, ?, ?, ?)";

    /**
     * Запрос на полное удаление блокировки чата из истории
     */
    private static final String REMOVE_MUTE_META = "DELETE FROM `MutesMeta` WHERE `Id` = ?";

    /**
     * Запрос на переход MuteMeta из активного в неактивное состояние
     * (размут), с указанием ника игрока снявшего мут и времени размута
     */
    private static final String FINISH_MUTE_META_EARLIER = "UPDATE `MutesMeta` SET `Expire` = ?, `Removed` = ? WHERE `Id` = ?";

    private static final String GET_ACTIVE_MUTES_COUNT = "SELECT COUNT(*) AS `count` FROM `MutesMeta` WHERE `Removed` IS NULL";

    private static final String GET_MUTES_FROM_DATE = "SELECT COUNT(*) AS `count` FROM `MutesMeta` WHERE `Time` >= ?";

    private final Database database = new MySQL(DatabaseCredentials.MAIN.withDatabase("newmutes"));

    //region Meta

    /**
     * Загрузка списка всех банов из базе данных
     *
     * @return - список банов
     */
    public CompletableFuture<List<MuteMeta>> loadMuteList() {
        return database.executeQuery(LOAD_MUTE_METAS)
                .thenApply(query -> {
                    List<MuteMeta> meta = new ArrayList<>(query.size());

                    for (BufferedRow row : query) {
                        //уникальный номер мута и id игрока получившего его
                        int muteId = row.getRequiredInt("Id");
                        int playerId = row.getRequiredInt("PlayerId");

                        long issueDate = row.getRequiredTimestamp("Time")
                                .atZone(ZoneId.systemDefault()).toEpochSecond() * 1000; // fixme

                        long expireTime = row.getTimestamp("Expire")
                                .map(time -> time.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000) // fixme
                                .orElse(PunishmentMeta.PERMANENT_TIME);

                        //прочая инфа о муте
                        String adminName = row.getRequiredString("Admin");

                        String unBanBy = row.getString("Removed")
                                .orElse(null);

                        String reason = row.getRequiredString("Reason");

                        //создание самой бан меты, добавление ее в общий список
                        meta.add(new MuteMeta(muteId, playerId, adminName, unBanBy, issueDate, expireTime, reason));
                    }

                    return meta;
                });
    }

    /**
     * Добавить информацию о новом муте в базу данных и получить его ID
     *
     * @param admin     - ник администратора, выдавшего мут
     * @param issueDate - время выдачи мута
     * @param time      - срок действия мута
     * @param reason    - причина мута
     * @return - информация о муте с его уникальным номером
     */
    public CompletableFuture<MuteMeta> addMuteMeta(int playerId, @NonNull String admin, long issueDate, long time,
                                                   @NonNull String reason) {
        return database.executeUpdate(ADD_MUTE_META, playerId, admin, new Timestamp(issueDate), new Timestamp(time), reason)
                .thenApply(query -> new MuteMeta(query.getGeneratedKey(), playerId, admin, issueDate, time, reason));
    }

    /**
     * Завершить активный мут досрочно администратором
     *
     * @param muteId   - уникальный id мута
     * @param unMuteBy - ник администратора, который снял мут
     */
    public void finishMuteMeta(int muteId, long expirationTime, @NonNull String unMuteBy) {
        database.executeVoidUpdate(FINISH_MUTE_META_EARLIER,
                new Timestamp(expirationTime), unMuteBy, muteId);
    }

    /**
     * Полностью удалить информацию о блокировке из БД
     *
     * @param banId - id блокировки
     */
    public void removeMuteMeta(int banId) {
        database.executeVoidUpdate(REMOVE_MUTE_META, banId);
    }

    public CompletableFuture<Long> getActiveMutesCount() {
        return database.executeQuery(GET_ACTIVE_MUTES_COUNT)
                .thenApply(query -> query.getFirst().getRequiredLong("count"));
    }

    public CompletableFuture<Long> getMutesFromDate(LocalDateTime time) {
        return getMutesFromDate(time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }

    public CompletableFuture<Long> getMutesFromDate(long dateMillis) {
        return database.executeQuery(GET_MUTES_FROM_DATE, new Timestamp(dateMillis))
                .thenApply(result -> result.getFirst().getRequiredLong("count"));
    }
}
