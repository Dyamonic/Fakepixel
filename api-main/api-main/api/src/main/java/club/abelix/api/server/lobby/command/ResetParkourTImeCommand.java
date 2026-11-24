package club.abelix.api.server.lobby.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.server.lobby.BaseLobbyServer;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.execution.Executor;
import lombok.AllArgsConstructor;

@NamedCommand("resetparkour")
@AllArgsConstructor
public class ResetParkourTImeCommand {

    BaseLobbyServer lobbyServer;

    @Executor
    public void execute(BukkitExecutionSession session) {
        lobbyServer.getLobbyParkourList().forEach(parkour -> {
            session.getGamer().removeProperty("Parkour/" + parkour.getName());
            session.sendMessage("Reset " + parkour.getName() + " time");
        });
    }

}
