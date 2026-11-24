package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.dependency.Dependency;
import club.abelix.commons.command.execution.ExecutionSession;
import club.abelix.commons.command.execution.Executor;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.connection.server.AbstractConnectable;
import club.abelix.core.app.connection.server.ServerManager;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@NamedCommand("servers")
public final class ServersCommand {

    @Dependency
    private static ServerManager manager;

    @Executor
    @WithPermission("command.servers")
    public static void executor(CoreExecutionSession session) {
        printServers("Proxy", manager.getProxies().stream()
                .sorted(Comparator.comparing(AbstractConnectable::getName))
                .collect(Collectors.toList()), session);

        printServers("Servers", manager.getServers().stream()
                .sorted(Comparator.comparing(AbstractConnectable::getName))
                .collect(Collectors.toList()), session);
    }

    private static void printServers(String category, List<AbstractConnectable> servers, ExecutionSession session) {
        if (servers.isEmpty()) {
            return;
        }

        session.sendMessage(" > " + category + " [" + servers.size() + "]: ");

        StringBuilder builder = new StringBuilder("   | ");

        int index = 0;
        Iterator<AbstractConnectable> serverIterator = servers.iterator();

        while (serverIterator.hasNext()) {
            AbstractConnectable nextServer = serverIterator.next();

            builder.append(nextServer.getName());

            if (++index == 8) {
                index = 0;
            } else if (serverIterator.hasNext()) {
                builder.append(", ");
                continue;
            }

            session.sendMessage(builder.toString());
            builder = new StringBuilder(" | ");
        }
    }

}
