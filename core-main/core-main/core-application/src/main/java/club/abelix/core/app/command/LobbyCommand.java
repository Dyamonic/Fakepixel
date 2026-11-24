package club.abelix.core.app.command;

import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.dependency.Dependency;
import club.abelix.commons.command.execution.Executor;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.connection.server.AbstractServer;
import club.abelix.core.app.connection.server.ArenaServer;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.network.type.mode.Lobby;
import club.abelix.network.type.mode.ServerMode;
import club.abelix.network.type.mode.Uncommon;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.val;

import java.util.List;

@WithAlias("l")
@WithAlias("д")
@WithAlias("leave")
@WithAlias("дщиин")
@WithAlias("лобби")
@IgnoreLogs
@NamedCommand("lobby")
public final class LobbyCommand {

    @Dependency
    private static ServerManager manager;

    @Executor
    @WithCondition("notBanned")
    @WithCondition("disableConsole")
    public static void execute(CoreExecutionSession session) {
        Gamer sender = session.getGamer();
        if (session.getArgumentCount() > 0) {
            val lobby = Lobbies.getByArgument(session.getArgumentString());
            if (lobby == null) {
                session.sendLocalizedMessage("lobby_command_lobbies_available", "%possible%", Lobbies.getPossibleArguments());
                return;
            }
            AbstractServer server = manager.getServer(lobby.getMode());
            sender.sendTo(server);
            return;
        }
        AbstractServer current = sender.getServer();

        AbstractServer server = null;
        if (current instanceof ArenaServer arena) {
            server = manager.getBestByOnline(arena.getMaster().getLobby(), false);
        }

        if (server != null) {
            sender.sendTo(server);
            return;
        }

        sendToLobby(session);

        // sender.dispatchCommand("/hub");
    }

   public static void sendToLobby(CoreExecutionSession session) {
        AbstractServer oldServer = session.getGamer().getServer();

        if (oldServer != null && oldServer.getMode() == Lobby.HUB) {
            session.getGamer().sendLocalizedMessage("core_hub_already_in");
            return;
        }

        AbstractServer server = manager.getBestByOnline(Lobby.HUB, false);

        if (server == null) {
            session.getGamer().sendLocalizedMessage("core_hub_not_found");
            return;
        }

        session.getGamer().sendTo(server);
    }

    @Getter
    @AllArgsConstructor
    enum Lobbies {

        DUEL(List.of("duels", "duel", "d", "duellobby", "duelslobby", "dlobby"), Lobby.DUELSLOBBY),
        BEDWARS(List.of("bw", "bwlobby", "b", "bedwarslobby", "blobby", "bedwars"), Lobby.BWLOBBY),
        ARCADE(List.of("ac", "aclobby", "a", "arcadelobby", "alobby", "arcade"), Lobby.ARCADELOBBY),
        BUILDFFA(List.of("bffa", "bffalobby", "bf", "buildffalobby", "bflobby", "buildffa"), Uncommon.BUILDFFA),
        FASTBRIDGE(List.of("fb", "fblobby", "f", "fastbridgelobby", "flobby", "fastbridge"), Uncommon.FASTBRIDGE),
        SKYBLOCK(List.of("sb" , "skyblock" , "skyblock_lobby") , Uncommon.SKYBLOCK);

        private final List<String> argument;
        private final ServerMode mode;

        public static Lobbies getByArgument(String arg) {
            for (Lobbies lobby : Lobbies.values()) {
                if (lobby.getArgument().contains(arg.toLowerCase())) {
                    return lobby;
                }
            }
            return null;
        }

        public static String getPossibleArguments() {
            StringBuilder possibleArguments = new StringBuilder();
            for (Lobbies lobby : Lobbies.values()) {
                possibleArguments.append(lobby.getArgument().toString()).append(", ");
            }
            // Удаляем последнюю запятую и пробел
            if (!possibleArguments.isEmpty()) {
                possibleArguments.delete(possibleArguments.length() - 2, possibleArguments.length());
            }
            return possibleArguments.toString();
        }

    }

}
