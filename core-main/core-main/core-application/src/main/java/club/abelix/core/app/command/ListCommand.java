package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.dependency.Dependency;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.commons.data.nickname.FakePlayerNickname;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.connection.server.AbstractServer;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.gamer.Gamer;
import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.PatternSyntaxException;

@NamedCommand("list")
public final class ListCommand {

    @Dependency
    private static ServerManager manager;

    @Executor
    @WithPermission("command.playerslist")
    @Tabcomplete("[serverName]")
    public static void execute(CoreExecutionSession session) throws CommandTranslationException {
        Set<AbstractServer> servers = new HashSet<>();

        if (session.getArgumentCount() == 0) {
            if (session.isConsole()) {
                throw new CommandTranslationException("core_list_format");
            }

            AbstractServer server = session.getGamer().getServer();

            if (server == null) {
                throw new CommandTranslationException("server_not_found");
            }

            servers.add(server);
        } else {
            try {
                Set<String> names = manager.findServerNames(session.getArgument(0));

                if (names.isEmpty()) {
                    throw new CommandTranslationException("servers_not_found");
                }

                for (String name : names) {
                    AbstractServer server = manager.getServer(name);

                    if (server != null) {
                        servers.add(server);
                    }
                }
            } catch (PatternSyntaxException e) {
                throw new CommandTranslationException("core_list_invalid_pattern", "%exception%", e.getMessage());
            }
        }

        List<String> playerNames = new ArrayList<>();

        for (AbstractServer server : servers) {
            for (Gamer gamer : server.getGamers()) {
                if (gamer.isVanished()) {
                    continue;
                }

                FakePlayerNickname nickname = gamer.getFakeNickname();

                playerNames.add(nickname == null ?
                        gamer.getDisplayName() : nickname.formatDisplayName());
            }
        }

        if (playerNames.isEmpty()) {
            throw new CommandTranslationException("core_no_online_players");
        }

        session.sendLocalizedMessage("core_list_result", "%players%", Joiner.on("§f, ").join(playerNames));
    }

}