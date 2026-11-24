package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.dependency.Dependency;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.util.information.BeautifulInformation;
import club.abelix.commons.util.information.BeautifulInformationLoader;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.connection.server.AbstractConnectable;
import club.abelix.core.app.connection.server.AbstractServer;
import club.abelix.core.app.connection.server.ArenaServer;
import club.abelix.core.app.connection.server.MasterServer;
import club.abelix.core.app.connection.server.MiniServer;
import club.abelix.core.app.connection.server.ServerManager;

import java.util.Collection;
import java.util.Comparator;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

@WithAlias("status")
@NamedCommand("serverinfo")
public final class StatusCommand {

    private static final BeautifulInformation<AbstractServer> INFORMATION
            = new BeautifulInformation<AbstractServer>("status_header") {
        {
            addDataLoader("information_server_name",
                    new BeautifulInformationLoader<>(AbstractConnectable::getName));

            addDataLoader("information_master_servers",
                    new BeautifulInformationLoader<>(MasterServer.class, master -> master.getServers().stream()
                            .map(MiniServer::getName).sorted().collect(Collectors.joining(", "))));

            addDataLoader("information_arena_map",
                    new BeautifulInformationLoader<>(ArenaServer.class, ArenaServer::getMapName));

            addDataLoader("information_arena_state",
                    new BeautifulInformationLoader<>(ArenaServer.class, ArenaServer::getState));

            addDataLoader("information_server_online",
                    new BeautifulInformationLoader<>(server -> server.getOnline() + "/" + server.getSlots()));
        }
    };

    @Dependency
    private static ServerManager manager;

    @Executor
    @WithCondition("minArgs: 1")
    @WithPermission("command.status")
    public static void execute(CoreExecutionSession session) throws CommandTranslationException {
        try {
            Collection<AbstractServer> servers = manager.findServers(session.getArgument(0));

            if (servers.isEmpty()) {
                throw new CommandTranslationException("core_servers_not_found");
            }

            StringBuilder builder = new StringBuilder();

            INFORMATION.printInformation(
                    servers.stream().sorted(Comparator.comparing(AbstractConnectable::getName))
                            .collect(Collectors.toList()), session.getLanguage(), builder
            );

            session.sendMessage(builder.toString());
        } catch (PatternSyntaxException e) {
            session.sendLocalizedMessage("core_list_invalid_pattern", "%exception%", e.getMessage());
        }
    }
}
