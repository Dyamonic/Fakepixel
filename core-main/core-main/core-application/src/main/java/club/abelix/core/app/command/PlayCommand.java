package club.abelix.core.app.command;

import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.connection.server.AbstractServer;
import club.abelix.core.app.connection.server.ArenaServer;
import club.abelix.core.app.connection.server.MiniServer;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.network.common.enums.GameState;
import club.abelix.network.type.mode.ServerMode;
import club.abelix.network.util.ModeUtil;

@IgnoreLogs
@NamedCommand("play")
public class PlayCommand {

    @Executor
    @WithCondition("disableConsole")
    @WithCondition("minArgs: 1")
    @Tabcomplete("[miniName]")
    public static void executor(CoreExecutionSession session) throws CommandTranslationException {
        String mode = session.getArgument(0);

        Gamer gamer = session.getGamer();

        AbstractServer server = ServerManager.IMP.getServer(mode);

        if (!(server instanceof MiniServer)) {
            ServerMode serverMode = ModeUtil.getServerModeByName(mode);

            if (!serverMode.isMini()) {
                throw new CommandTranslationException("command_play_invalid_mode");
            }

            gamer.sendTo(serverMode);
        } else {
            if (((ArenaServer) server).getState() != GameState.STAND_BY) {
                throw new CommandTranslationException("command_play_server_started");
            }

            if (server.getSpace() == 0) {
                throw new CommandTranslationException("command_play_server_full");
            }

            gamer.sendTo(server);

            gamer.sendLocalizedMessage("command_play_teleport_server",
                    "%server%", server.getName());
        }
    }
}
