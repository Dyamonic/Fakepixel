package club.abelix.api.server.lobby.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.server.lobby.BaseLobbyServer;
import club.abelix.api.server.lobby.event.PlayerTeleportToSpawnEvent;
import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.execution.Executor;
import club.abelix.core.bukkit.HeartBeat;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.jetbrains.annotations.NotNull;

@NamedCommand("spawn")
@IgnoreLogs
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SpawnCommand {

    BaseLobbyServer server;

    public static @NotNull SpawnCommand create(final @NonNull BaseLobbyServer server) {
        return new SpawnCommand(server);
    }

    @Executor
    @WithCondition("disableConsole")
    public void execute(final BukkitExecutionSession session) {
        val player = session.getPlayer();

        val event = PlayerTeleportToSpawnEvent.create(player);
        HeartBeat.callEvent(event);

        if (event.isCancelled()) {
            return;
        }

        player.teleport(server.getSpawnLocation());
        session.sendLocalizedMessage("command_spawn_result");
    }
}
