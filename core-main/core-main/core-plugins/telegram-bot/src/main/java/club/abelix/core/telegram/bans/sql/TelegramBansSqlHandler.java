package club.abelix.core.telegram.bans.sql;

import club.abelix.commons.data.punishment.meta.PunishmentMeta;
import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import club.abelix.core.telegram.bans.meta.TelegramBanMeta;
import lombok.NonNull;
import lombok.val;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TelegramBansSqlHandler {

    public static final TelegramBansSqlHandler INSTANCE = new TelegramBansSqlHandler();

    //-------------------------------------------------------------//
    //                      Для системы банов                      //
    //-------------------------------------------------------------//
    /**
     * Запрос на загрузку всех банов
     */
    private static final String LOAD_BAN_METAS = "SELECT * FROM `BotBans`";

    /**
     * Запрос на добавление новой BanMeta
     */
    private static final String ADD_BAN = "INSERT INTO `BotBans` (`Telegram`, `Time`, `Expire`, `Reason`) VALUES (?, ?, ?, ?)";

    /**
     * Запрос на полное удаление бана
     */
    private static final String REMOVE_BAN_BY_ID = "DELETE FROM `BotBans` WHERE `Id` = ?";
    private static final String REMOVE_BAN_BY_TELEGRAM = "DELETE FROM `BotBans` WHERE `Telegram` = ?";

    private final Database database = new MySQL(DatabaseCredentials.MAIN.withDatabase("TelegramBot"));

    /**
     * Загрузка списка всех банов из базе данных
     *
     * @return - список банов
     */
    public CompletableFuture<List<TelegramBanMeta>> loadBanList() {
        return database.executeQuery(LOAD_BAN_METAS).thenApply(result -> {
            val banMetaList = new ArrayList<TelegramBanMeta>(result.size());

            for (val row : result) {
                val banId = row.getRequiredInt("Id");
                val telegramId = row.getRequiredLong("Telegram");

                val expireTime = row.getTimestamp("Expire")
                        .map(time -> time.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000) // fixme
                        .orElse(PunishmentMeta.PERMANENT_TIME);

                val banTime = row.getRequiredTimestamp("Time")
                        .atZone(ZoneId.systemDefault()).toEpochSecond() * 1000;

                val reason = row.getRequiredString("Reason");
                val meta = new TelegramBanMeta(banId, telegramId, reason, banTime, expireTime);

                if (meta.isExpired()) {
                    removeBanMeta(banId);
                    continue;
                }

                banMetaList.add(meta);
            }

            return banMetaList;
        });
    }

    public CompletableFuture<TelegramBanMeta> addBanMeta(long telegram, long time, long expire, @NonNull String reason) {
        return database.executeUpdate(ADD_BAN, telegram, new Timestamp(time),
                        expire != PunishmentMeta.PERMANENT_TIME ? new Timestamp(expire) : null, reason)
                .thenApply(query -> new TelegramBanMeta(query.getGeneratedKey(), telegram, reason, time, expire));
    }

    /**
     * Полностью удалить информацию о блокировке из БД
     *
     * @param banId - id блокировки
     */
    public void removeBanMeta(int banId) {
        database.executeVoidUpdate(REMOVE_BAN_BY_ID, banId);
    }

    /**
     * Полностью удалить информацию о блокировке из БД
     *
     * @param telegram - id блокировки
     */
    public void removeBanMeta(long telegram) {
        database.executeVoidUpdate(REMOVE_BAN_BY_TELEGRAM, telegram);
    }

    // endregion

}
