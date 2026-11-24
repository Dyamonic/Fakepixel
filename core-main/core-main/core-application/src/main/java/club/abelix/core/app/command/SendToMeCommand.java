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
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.network.type.connect.ConnectStatus;

@NamedCommand("sendtome")
public final class SendToMeCommand {

    @Dependency
    private static ServerManager manager;

    @Executor
    @Tabcomplete("[playerName]")
    @WithPermission("command.sendtome")
    @WithCondition("minArgs: 1")
    public static void doExecution(CoreExecutionSession session) throws CommandTranslationException {
        Gamer target = Gamer.getGamer(session.getArgument(0));
        if (target == null) {
            throw new CommandTranslationException("player_not_found");
        }
        AbstractServer server = manager.getServer(session.getGamer().getCurrentServerName());

        if (server == null || server instanceof MasterServer) {
            throw new CommandTranslationException("server_not_found");
        }

        target.sendTo(server).thenAccept(status -> {
            if (status == ConnectStatus.SUCCESS) {
                session.sendLocalizedMessage("core_send_sender_result",
                        "%name%", target.getDisplayName(), "%server_name%", server.getName());
            }
        });
    }

}
