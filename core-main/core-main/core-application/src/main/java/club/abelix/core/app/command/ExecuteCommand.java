package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.core.app.CoreApplication;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.connection.server.AbstractServer;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.event.core.RemoteClientCommandExecuteEvent;
import club.abelix.network.packet.play.server.SServerCommand;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@NamedCommand("execute")
public class ExecuteCommand {

    private final CoreApplication core;

    @Executor
    @WithPermission("command.exec")
    @WithCondition("permission: command.execute")
    @WithCondition("minArgs: 2")
    public void main(CoreExecutionSession session) throws CommandTranslationException {
        String command = session.getArgumentString(1);

        AbstractServer server = ServerManager.IMP.getServer(session.getArgument(0));

        if (server == null) {
            throw new CommandTranslationException("server_not_found");
        }

        // для оповещения в вк об исполнении команды
        RemoteClientCommandExecuteEvent event
                = new RemoteClientCommandExecuteEvent(session.getSender(), server, command);
        core.getPluginManager().callEvent(event);

        server.sendPacket(new SServerCommand(command));

        session.sendLocalizedMessage("command_execute_success",
                "%server%", server.getName());
    }

}
