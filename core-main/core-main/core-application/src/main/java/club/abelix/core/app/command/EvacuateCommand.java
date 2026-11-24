package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.execution.Executor;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.network.type.mode.Lobby;
import club.abelix.network.type.server.state.ServerState;
import lombok.val;

@NamedCommand("evacuate")
public class EvacuateCommand {

    @Executor
    public void evacuate(CoreExecutionSession session) {
        val gamer = session.getGamer();
        val server = gamer.getServer();

        if (server.getServerState() != ServerState.SCHEDULED_RESTART) {
            session.sendLocalizedMessage("cant_evacuate");
            return;
        }

        gamer.sendTo(Lobby.HUB);
        gamer.sendLocalizedMessage("evacuated_to_hub");
    }

}
