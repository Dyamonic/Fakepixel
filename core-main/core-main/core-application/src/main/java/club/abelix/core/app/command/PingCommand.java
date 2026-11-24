package club.abelix.core.app.command;

import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.commons.util.color.ChatColor;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.network.common.logging.Logging;
import club.abelix.network.packet.play.server.SPingRequest;
import lombok.NonNull;
import lombok.val;

@IgnoreLogs
@NamedCommand("ping")
public final class PingCommand {

    @Executor
    @Tabcomplete("[playerName]")
    public static void execute(@NonNull final CoreExecutionSession session) throws CommandTranslationException {
        Gamer target;
        if (session.getArgumentCount() != 0) {
            val targetGamer = session.getGamer(0);

            if (session.getSender().hasPermission("command.ping.others") || session.isConsole()) {
                target = targetGamer;
            } else {
                session.sendLocalizedMessage("command_unavailable_group");
                return;
            }
        } else {
            target = session.getGamer();
        }

        processPing(session, target);
    }

    private static void processPing(@NonNull CoreExecutionSession session, @NonNull Gamer target) {
        target.getProxy()
                .getConnection()
                .awaitPacket(new SPingRequest(target.getPlayerId()))
                .whenComplete((cPingResponse, throwable) -> {
                    if (throwable != null) {
                        Logging.IMP.error("[Ping] Error processed ping from {}. Details: {}",
                                target.getName(), throwable);
                        session.sendLocalizedMessage("internal_error");
                        return;
                    }

                    if (cPingResponse.getPing() == -1) {
                        session.sendLocalizedMessage("internal_error");
                        return;
                    }

                    session.sendLocalizedMessage("ping_command_response",
                            "%ping%", getColoredPing(cPingResponse.getPing()),
                            "%target%", target.getDisplayName());
                });
    }

    private static String getColoredPing(long ping) {
        ChatColor color;
        if (ping <= 80) {
            color = ChatColor.GREEN;
        } else if (ping <= 145) {
            color = ChatColor.YELLOW;
        } else {
            color = ChatColor.RED;
        }
        return "§" + color.getCode() + " " + ping;
    }

}
