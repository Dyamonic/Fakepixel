package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
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

@WithAlias("send-all")
@NamedCommand("sendall")
public final class SendAllCommand {

    @Dependency
    private static ServerManager manager;

    @Executor
    @Tabcomplete("[serverName]")
    @WithPermission("command.sendall")
    @WithCondition("safeProduction")
    @WithCondition("minArgs: 1")
    public static void execute(CoreExecutionSession session) throws CommandTranslationException {
        AbstractServer server = manager.getServer(session.getArgument(0));

        if (server == null || server instanceof MasterServer) {
            throw new CommandTranslationException("server_not_found");
        }

        if (server.getSlots() < 1) {
            throw new CommandTranslationException("server_unknown_slots");
        }

        int needSend = server.getSlots() - server.getOnline(true);

        if (needSend <= 0) {
            throw new CommandTranslationException("server_already_full");
        }

        for (Gamer gamer : Gamer.getGamers()) {
            if (gamer.getServer() != server) {
                gamer.sendTo(server).thenAccept(status -> {
                    if (status == ConnectStatus.SUCCESS) {
                        gamer.sendLocalizedMessage("core_send_player_result",
                                "%server_name%", server.getName(), "%name%", session.getDisplayName());
                    }
                });
            }
        }

        session.sendLocalizedMessage("core_sendall_result", "%count%", Gamer.getGamers().size());
    }

}
