package club.abelix.core.app.data.stats;

import club.abelix.commons.data.currency.GameCurrency;
import club.abelix.commons.data.punishment.meta.BanMeta;
import club.abelix.commons.data.purchase.PurchaseSqlHandler;
import club.abelix.commons.data.stat.StatsManager;
import club.abelix.commons.data.stat.StatsMode;
import club.abelix.commons.data.stat.StatsSqlHandler;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.time.TimeUtil;
import club.abelix.core.app.api.command.CommandSender;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.network.common.logging.Logging;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * @author Unidentified Person
 */
public final class StatsWipeManager {

    public static final StatsWipeManager INSTANCE = new StatsWipeManager();

    public static final long MIN_BAN_TIME = TimeUnit.DAYS.toDays(2);

    public void runTimer() {
        Timer timer = new Timer("StatsWipe");

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                StatsWipeSqlHandler.WipingStatEntry[] entries
                        = StatsWipeSqlHandler.INSTANCE.select().join();

                for (StatsWipeSqlHandler.WipingStatEntry entry : entries) {
                    int id = entry.getPlayerId();
                    int fromDay = entry.getFromDay();

                    Gamer gamer = Gamer.getGamer(id);

                    if (gamer != null) {
                        gamer.localizedKick("ban_stat_wipe_kick");
                    }

                    LocalDate fromDate = LocalDate.ofEpochDay(fromDay);

                    for (StatsMode mode : StatsManager.INSTANCE.getModes()) {
                        StatsSqlHandler.INSTANCE.deleteStats(id, mode, fromDay);
                    }

                    if (fromDay > 0) {
                        PurchaseSqlHandler.INSTANCE.removeRecentPurchases(id, fromDate, GameCurrency.COINS);
                    }

                    StatsWipeSqlHandler.INSTANCE.delete(id);

                    Logging.IMP.info("Статистика пользователя (ID #{}) была очищена (начиная с {})",
                            id, TimeUtil.getDateString(Language.defaultLanguage(), fromDate));
                }

                if (entries.length > 0) {
                    ServerManager.IMP.broadcastStaff("command.staffchat", "ban_stat_wipe_removed",
                            "%count%", entries.length);
                }
            }
        }, TimeUnit.SECONDS.toMillis(15), TimeUnit.HOURS.toMillis(1));
    }

    public void handleUserUnbanned(BanMeta meta, CommandSender admin) {
        if (!meta.getUntranslatedReason().contains("2.5")) return;
        if (admin.isConsole()) return;

        int[] bannedIds = meta.getBannedIds();

        // Забанили айпишник или игроки не найдены
        if (bannedIds.length == 0) {
            return;
        }

        StatsWipeSqlHandler.INSTANCE.delete(bannedIds);
    }

    public void handleUserBanned(BanMeta meta) {
        long time = meta.getExpirationDate() - meta.getIssueDate();

        if ((!meta.isPermanent() && time < MIN_BAN_TIME) || !meta.getUntranslatedReason().contains("2.5")) return;

        int[] bannedIds = meta.getBannedIds();

        // Забанили айпишник или игроки не найдены
        if (bannedIds.length == 0) {
            return;
        }

        // стата вайпается от момента бана - 10 дней
        int wipeDay = (int) (TimeUtil.getDayUTC() - 10);

        LocalDateTime wipeTime;

        if (meta.isPermanent()) {
            wipeTime = LocalDateTime.now().plusDays(5);
        } else {
            wipeTime = LocalDateTime.now().plus(time, ChronoUnit.MILLIS).minusHours(6);
        }

        for (int bannedId : bannedIds) {
            StatsWipeSqlHandler.INSTANCE.insert(bannedId, wipeDay, wipeTime);
        }
    }

}
