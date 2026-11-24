package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.execution.Executor;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.network.common.enums.ServerStatus;
import club.abelix.network.packet.play.server.SServerStatusUpdate;
import lombok.val;

/**
 * @author ildar (quiu)
 * @since 2/15/2024 at 4:38 PM</p>
 * For core project.
 */

@NamedCommand("serverjoin")
public final class CloseJoinCommand {

    @Executor
    @WithPermission("command.serverjoin")
    public static void execute(CoreExecutionSession session) {
        try {
            val status = ServerStatus.valueOf(session.getArgument(0));
            ServerManager.IMP.getProxies().forEach(proxy -> {
                proxy.getConnection().sendPacket(new SServerStatusUpdate(status));
            });
            session.sendMessage("Proxies status updated to " + status.name() + "!");
        } catch (IllegalArgumentException e) {
            session.sendMessage("Invalid status! Possible values: OPENED, CLOSED");
        }

    }
}
