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

@NamedCommand("sendfrom")
public final class SendFromCommand {

    @Dependency
    private static ServerManager manager;

    @Executor
    @Tabcomplete("[serverName] [serverName]")
    @WithPermission("command.sendfrom")
    @WithCondition("minArgs: 2")
    public static void execute(CoreExecutionSession session) throws CommandTranslationException {
        AbstractServer fromServer = manager.getServer(session.getArgument(0));
        AbstractServer toServer = manager.getServer(session.getArgument(1));

        if ((toServer == null || toServer instanceof MasterServer)
                || (fromServer == null || fromServer instanceof MasterServer)) {
            throw new CommandTranslationException("server_not_found");
        }

        if (toServer.getSlots() < 1) {
            throw new CommandTranslationException("server_unknown_slots");
        }

        int needSend = toServer.getSlots() - toServer.getOnline(true);

        if (needSend <= 0) {
            throw new CommandTranslationException("server_already_full");
        }

        int sent = 0;

        for (Gamer gamer : fromServer.getGamers()) {
            gamer.sendTo(toServer);
            gamer.sendLocalizedMessage("core_send_player_result",
                    "%server_name%", toServer.getName(), "%name%", session.getDisplayName());

            if (++sent == needSend) {
                break;
            }
        }

        session.sendLocalizedMessage("core_sendfrom_result", "%server%", toServer.getName(), "%count%", sent);
    }

}
