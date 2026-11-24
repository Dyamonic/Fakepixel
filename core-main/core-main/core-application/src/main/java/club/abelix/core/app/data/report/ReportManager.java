package club.abelix.core.app.data.report;

import club.abelix.commons.data.stat.staff.StaffStatsSqlHandler;
import club.abelix.commons.language.LanguageContext;
import club.abelix.commons.util.array.ObjectLinkedBuf;
import club.abelix.core.app.Async;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.data.punishment.PunishmentManager;
import club.abelix.core.app.data.punishment.exception.PunishmentException;
import club.abelix.core.app.data.report.meta.Report;
import club.abelix.network.type.mode.ServerMode;
import club.abelix.network.type.mode.Special;
import club.abelix.network.type.report.ReportReason;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ReportManager {

    // --------------------------------------------------------
    public static final ReportManager INSTANCE = new ReportManager();
    // --------------------------------------------------------

    private final TIntObjectMap<Report> reportMap
            = new TIntObjectHashMap<>();

    /**
     * Проверка возможности отправить репорт на игрока
     *
     * @param sender - отправитель жалобы
     * @param target - тот, на кого жалуются
     * @return - true, если можно отправить жалобу; false, если нет
     */
    public boolean checkReportSendState(@NotNull Gamer sender, Gamer target) {
        if (sender.getCooldown("ReportCooldown").isActive()) {
            sender.sendLocalizedMessage("report_cooldown", "%duration%", sender.getCooldown("ReportCooldown").getAvailableAt());
            return false;
        }

        if (target == null || target.isVanished()) {
            sender.sendLocalizedMessage("player_offline");
            return false;
        }

        if (target.isHelper()) {
            sender.sendLocalizedMessage("report_send_staff");
            return false;
        }

        ServerMode mode = target.getServer().getMode();

        if (mode == Special.LIMBO || mode == Special.LOGIN) {
            sender.sendLocalizedMessage("report_invalid_mode");
            return false;
        }

        Report report = getReport(target.getPlayerId());

        if (report != null && report.hasSender(sender)) {
            sender.sendLocalizedMessage("report_send_already_reported");
            return false;
        }

        return true;
    }

    /**
     * Отправить жалобу на игрока с определенной причиной
     *
     * @param sender - отправитель жалобы
     * @param target - тот, на кого жаловались
     * @param reason - причина жалобы
     */
    public void sendReport(@NotNull Gamer sender, Gamer target, @NotNull ReportReason reason) {
        if (target == null) {
            return;
        }

        Report report = getReport(target);

        if (report == null) {
            addReport(report = new Report(target));
        }

        sender.sendLocalizedMessage("report_send_success", "%reason%", reason.getReason(sender.getLanguage()));

        // персонал оповещаем только если последний раз оповещали больше 2 минут назад
        if (System.currentTimeMillis() - report.getLastReportTime()
                > TimeUnit.MINUTES.toMillis(2)) {

            ServerManager.IMP.broadcastGroup(
                    "command.staffchat",
                    new LanguageContext(
                            "report_new_report",
                            language -> new ObjectLinkedBuf(
                                    "%target%", target.getDisplayName(),
                                    "%name%", sender.getDisplayName(),
                                    "%reason%", reason.getReason(language),
                                    "%server%", target.getServer().getName()
                            )
                    )
            );
        }

        report.addSender(sender, reason);

        sender.setCooldown("ReportCooldown", 15, TimeUnit.SECONDS);
    }

    /**
     * Закрыть репорт на игрока
     *
     * @param admin  администратор, рассмотревший репорт
     * @param target игрок, на которого подавали репорт
     * @param reason причина, по которой администратор решил наказать игрока
     *               UNKNOWN, если репорт нужно отклонить
     */
    public void closeReport(@NotNull Gamer admin, @NotNull Gamer target, @Nullable ReportReason reason) {
        Report report = removeReport(target);

        if (report == null) {
            admin.sendLocalizedMessage("report_already_closed");
            return;
        }

        boolean guilty = reason != null;

        for (Gamer sender : report.getSenders().keySet()) {
            if (guilty) {
                sender.sendLocalizedMessage("report_your_report_accepted");
            }
        }

        int adminPlayerId = admin.getPlayerId();

        StaffStatsSqlHandler.INSTANCE.updateStaffStats(adminPlayerId, 1, 0);

        if (guilty) {
            try {
                PunishmentManager.INSTANCE.processBan(
                        admin, target.getName(), reason.getReason(admin.getLanguage()), reason.getDurationString()
                );
            } catch (PunishmentException e) {
                e.printMessages(admin);
            }
        }
    }

    public void spectate(@NotNull Gamer gamer, @NotNull Gamer target) {
        Report report = getReport(target.getPlayerId());

        if (report == null) {
            return;
        }

        if (!gamer.isVanished()) {
            gamer.sendLocalizedMessage("report_spectating_vanish", "%target%", target.getDisplayName());
            gamer.setVanished(true);
        }

        Async.runLater(() -> {
            if (target.getServer() != null) {
                gamer.sendTo(target.getServer());
            }
        }, 1, TimeUnit.SECONDS);
    }

    /**
     * Получить репорт по ID нарушителя
     *
     * @param playerId id нарушителя
     * @return Репорт, если его нет, то null
     */
    @Nullable
    public Report getReport(int playerId) {
        synchronized (reportMap) {
            return reportMap.get(playerId);
        }
    }

    /**
     * Получить репорт по нарушителю
     *
     * @param user Нарушитель
     * @return Репорт, если его нет, то null
     * @see #getReport(int)
     */
    @Nullable
    public Report getReport(Gamer user) {
        return getReport(user.getPlayerId());
    }

    public Report removeReport(Gamer user) {
        return removeReport(user.getPlayerId());
    }

    public Report removeReport(int playerId) {
        synchronized (reportMap) {
            return reportMap.remove(playerId);
        }
    }

    public boolean hasReport(Gamer user) {
        return hasReport(user.getPlayerId());
    }

    public boolean hasReport(int playerId) {
        synchronized (reportMap) {
            return reportMap.containsKey(playerId);
        }
    }

    public void addReport(@NotNull Report report) {
        synchronized (reportMap) {
            reportMap.put(report.getGamer().getPlayerId(), report);
        }
    }

    /**
     * Получить список жалоб
     *
     * @return - список жалоб на игроков
     */
    public List<Report> getReports() {
        synchronized (reportMap) {
            return new ArrayList<>(reportMap.valueCollection());
        }
    }

}
