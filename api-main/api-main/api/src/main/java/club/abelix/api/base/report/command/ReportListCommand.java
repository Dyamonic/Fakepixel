package club.abelix.api.base.report.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.report.gui.staff.ReportListGui;
import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.execution.SubcommandExecutor;
import club.abelix.commons.data.stat.staff.StaffStatsSqlHandler;

@NamedCommand("reports")
@IgnoreLogs
public class ReportListCommand {
    
    @Executor
    @WithPermission("command.reports")
    @WithCondition("disableConsole")
    public static void executor(BukkitExecutionSession session) {
        new ReportListGui(session.getPlayer());
    }
    
    @WithCondition("minArgs: 1")
    @WithPermission("command.reports.stats")
    @WithCondition("disableConsole")
    @SubcommandExecutor("stats")
    public static void stats(BukkitExecutionSession session) throws CommandException {
        var offlineTargetGamer = session.getOfflineGamer(0, Gamer::loadPlayerId);
        
        StaffStatsSqlHandler.INSTANCE.loadStaffStats(offlineTargetGamer.getPlayerId()).thenAccept(staffStats -> {
            session.sendLocalizedMessage("reports_staff_stats",
                "%player%", offlineTargetGamer.getName(),
                "%closed_reports%", staffStats.getClosedReposts(),
                "%punishments%", staffStats.getPunishmentReports()
            );
        });
    }
}
