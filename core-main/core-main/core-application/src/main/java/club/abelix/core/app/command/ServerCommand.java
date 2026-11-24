package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.dependency.Dependency;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.connection.server.AbstractServer;
import club.abelix.core.app.connection.server.MasterServer;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.network.type.mode.ServerMode;
import club.abelix.network.type.mode.Special;

@NamedCommand("server")
public final class ServerCommand {

    @Dependency
    private static ServerManager manager;

    @Executor
    @Tabcomplete("[serverName]")
    @WithCondition("minArgs: 1")
    @WithPermission("command.server")
    @WithCondition("disableConsole")
    @WithCondition("notBanned")
    public static void executor(CoreExecutionSession session) throws CommandTranslationException {
        String name = session.getArgument(0).toLowerCase();
        AbstractServer server = manager.getServer(name);
        session.getGamer().sendMessage("[DEBUG] Is server available in list : " + manager.hasServer(name));

        if (server == null || server instanceof MasterServer) {
            throw new CommandTranslationException("server_not_found");
        }

        ServerMode mode = server.getMode();

        if ((mode == Special.LIMBO || mode == Special.LOGIN)
                && !session.getSender().hasPermission("command.server.bypass")) {
            throw new CommandTranslationException("server_teleportation_rejected");
        }

        session.sendLocalizedMessage("server_teleportation_started", "%server_name%", server.getName());
        session.getGamer().sendTo(server);
    }

}
