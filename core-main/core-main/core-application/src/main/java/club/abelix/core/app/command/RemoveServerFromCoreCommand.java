package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.execution.Executor;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.connection.server.AbstractServer;
import club.abelix.core.app.connection.server.ServerManager;

import java.io.IOException;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>RemoveServerFromCoreCommand creation on 24.03.2023 at 21:40</p>
 */

@NamedCommand("removeserverfromcore")
public class RemoveServerFromCoreCommand {
    @Executor
    @WithPermission("command.removeserverfromcore")
    @WithCondition("minArgs: 1")
    public static void removeServerFromCore(CoreExecutionSession session) throws IOException {
        String serverName = session.getArgument(0);

        AbstractServer server = ServerManager.IMP.getServer(serverName);

        if (server == null) {
            session.sendMessage("§cServer not found!");
            return;
        }

        ServerManager.IMP.removeServer(server);

        session.sendMessage("§eServer has been removed from core!");
    }
}
