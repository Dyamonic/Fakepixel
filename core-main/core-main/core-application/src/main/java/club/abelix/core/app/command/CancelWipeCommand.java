package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandException;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.data.punishment.meta.BanMeta;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.punishment.PunishmentManager;
import club.abelix.core.app.data.stats.StatsWipeSqlHandler;

/**
 * @author Unidentified Person
 */
@NamedCommand("cancelwipe")
public class CancelWipeCommand {

    @Executor
    @WithPermission("command.cancelwipe")
    @WithCondition("minArgs: 1")
    public static void invoke(CoreExecutionSession session) throws CommandException {
        int banId = session.getIntArgument(0, "Ban ID");

        BanMeta ban = PunishmentManager.INSTANCE.findBanMeta(banId)
                .orElseThrow(() -> new CommandTranslationException("banidinfo_empty_result"));

        int[] banIds = ban.getBannedIds();

        if (banIds.length == 0) {
            throw new CommandTranslationException("command_cancelwipe_no_bans");
        }

        StatsWipeSqlHandler.INSTANCE.deleteAndCheck(banIds)
                .whenComplete((cancelledCount, cause) -> {
                    if (cause != null) {
                        session.sendLocalizedMessage("command_cancelwipe_fail");
                        return;
                    }

                    if (cancelledCount == 0) {
                        session.sendLocalizedMessage("command_cancelwipe_empty");
                    } else {
                        ServerManager.IMP.broadcastStaff("command_cancelwipe_broadcast",
                                "%id%", ban.getId(), "%admin%", session.getDisplayName(), "%count%", cancelledCount);
                    }
                });
    }

}
