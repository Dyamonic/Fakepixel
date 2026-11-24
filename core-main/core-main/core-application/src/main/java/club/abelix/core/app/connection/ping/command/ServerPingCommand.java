package club.abelix.core.app.connection.ping.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.execution.Executor;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.connection.ping.ServerPingService;
import club.abelix.core.app.connection.server.ServerManager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;

import java.util.concurrent.ThreadLocalRandom;

@NamedCommand("serverping")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public final class ServerPingCommand {

    ServerPingService pingService;
    ServerManager serverManager;

    @Executor
    @WithPermission("command.serverping")
    public void execute(CoreExecutionSession session) {
        val server = serverManager.getServer(session.getArgument(0));
        if (server == null) {
            session.sendMessage("Server not found.");
            return;
        }

        val randomId = ThreadLocalRandom.current().nextInt();
        pingService.requestPing(server, randomId)
                .thenAccept(ping -> session.sendMessage("Ping to %s: %d ms".formatted(server.getName(), ping)))
                .exceptionally(ex -> {
                    session.sendMessage("Failed to ping server: " + ex.getMessage());
                    return null;
                });
    }

}
