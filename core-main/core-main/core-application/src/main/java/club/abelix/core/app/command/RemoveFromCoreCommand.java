package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.execution.Executor;
import club.abelix.core.app.CoreApplication;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.connection.RemoteClientConnection;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.network.common.logging.Logging;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
@WithAlias("rfc")
@NamedCommand("removefromcore")
public final class RemoveFromCoreCommand {

    @Executor
    @WithCondition("minArgs: 1")
    @WithPermission("command.removefromcore")
    public static void removeFromCore(CoreExecutionSession session) {
        String playerName = session.getArgument(0);
        Gamer gamer = Gamer.getGamer(playerName);
        RemoteClientConnection.PENDING_LOGINS.remove(playerName.toLowerCase());

        if (gamer != null) {
            gamer.quit();
        }

        CoreApplication.getInstance()
                .getSessionService()
                .closeSession(playerName.toLowerCase());

        ServerManager.IMP.broadcastStaff(
                "command.staffchat",
                "core_player_remove",
                "%sender%",
                session.getDisplayName(),
                "%target%",
                playerName
        );

        log.info(
                "Player {} has been removed from core by {}!",
                playerName,
                session.getGamer().getName()
        );

        session.sendMessage("§ePlayer has been removed from core!");
    }

}
