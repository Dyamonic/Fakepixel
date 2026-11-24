package club.abelix.api.base.report.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.report.gui.player.ReportGui;
import club.abelix.api.base.report.util.ReportUtil;
import club.abelix.commons.command.Async;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.core.bukkit.connection.Connection;
import club.abelix.network.packet.play.client.CReportAction;
import club.abelix.network.type.report.ReportReason;

@NamedCommand("report")
@WithAlias("sendreport")
@WithAlias("cheatreport")
@WithAlias("wdr")
@WithAlias("watchdogreport")
public class ReportCommand {

    @Async
    @Executor
    @WithCondition("disableConsole")
    @WithCondition("minArgs: 1")
    public static void executor(BukkitExecutionSession session) throws CommandTranslationException {
        Gamer gamer = session.getGamer();
        String name = session.getArgument(0);

        String reason;

        if (session.getArgumentCount() > 1){
            reason = session.getArgument(1);
        } else {
            reason = null;
        }

        if (gamer.getName().equalsIgnoreCase(name)) {
            throw new CommandTranslationException("report_self");
        }

        Gamer target = Gamer.getOrCreate(name, Gamer::loadRanks);

        if (target == null) {
            throw new CommandTranslationException("player_not_found");
        }

        if (target.getRanks().isStaff()) {
            throw new CommandTranslationException("cannot_report_staff");
        }

        Connection.checkReportSendState(gamer.getPlayerId(), target.getPlayerId())
                .whenComplete((response, cause) -> {
                    if (!response) {
                        return;
                    }

                    Gamer targetGamer = Gamer.getOrCreate(name, loadedGamer -> {
                        loadedGamer.loadPlayerId();
                        loadedGamer.loadRanks();
                        loadedGamer.updateDisplayName();
                    });

                    // Если игрок указал причину репорта к примеру /report pro0xy hacking то репортим без открывания меню
                    if (reason != null){
                        ReportReason reportReason = ReportUtil.getReportByReason(reason);
                        if (reportReason != null){
                            Connection.sendPacket(
                                    new CReportAction(
                                            CReportAction.Action.SEND,
                                            gamer.getPlayerId(), target.getPlayerId(), reportReason
                                    )
                            );
                            return;
                        }
                    }

                    new ReportGui(gamer, targetGamer);
                });
    }
}
