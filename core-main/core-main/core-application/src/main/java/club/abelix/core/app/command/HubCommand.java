package club.abelix.core.app.command;

import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.dependency.Dependency;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.connection.server.AbstractServer;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.network.type.mode.Lobby;
import club.abelix.network.type.mode.ServerMode;
import club.abelix.network.type.mode.ServerType;
import club.abelix.network.type.mode.Uncommon;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@WithAlias("рги")
@WithAlias("хаб")
@IgnoreLogs
@NamedCommand("hub")
public final class HubCommand {

    private static final Set<Uncommon> SKYBLOCK_MODES = EnumSet.of(
            Uncommon.SKYBLOCK,
            Uncommon.SBI,
            Uncommon.SBJ,
            Uncommon.SBD,
            Uncommon.SBM,
            Uncommon.SBDUNG,
            Uncommon.SBDH,
            Uncommon.SBSPIDERS,
            Uncommon.SBCRIS,
            Uncommon.SBPARK,
            Uncommon.SBEND,
            Uncommon.SBFARMS,
            Uncommon.SBMINES,
            Uncommon.SBCH,
            Uncommon.SKYBLOCK_P
    );

    @Dependency
    private static ServerManager manager;

    @Executor
    @WithCondition("notBanned")
    @WithCondition("disableConsole")
    public static void execute(CoreExecutionSession session) throws CommandTranslationException {
        AbstractServer oldServer = session.getGamer().getServer();

        if (oldServer != null && isSkyBlock(oldServer.getMode())){
            if (oldServer.getMode() == Uncommon.SKYBLOCK) {
                session.getGamer().sendLocalizedMessage("core_hub_already_in");
                return;
            }
            session.getGamer().sendTo(Uncommon.SKYBLOCK);
            return;
        }

        LobbyCommand.sendToLobby(session);

//        if (oldServer != null && oldServer.getMode() == Lobby.HUB) {
//            session.getGamer().sendLocalizedMessage("core_hub_already_in");
//            return;
//        }
//
//        AbstractServer server = manager.getBestByOnline(Lobby.HUB, false);
//
//        if (server == null) {
//            session.getGamer().sendLocalizedMessage("core_hub_not_found");
//            return;
//        }
//
//        session.getGamer().sendTo(server);
    }

    private static boolean isSkyBlock(ServerMode type) {
        if (type == null) return false;
        return type instanceof Uncommon && SKYBLOCK_MODES.contains(type);
    }

}
