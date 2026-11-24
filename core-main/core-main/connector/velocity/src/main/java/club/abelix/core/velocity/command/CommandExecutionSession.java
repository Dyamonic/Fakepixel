package club.abelix.core.velocity.command;

import club.abelix.commons.command.execution.ExecutionSession;
import club.abelix.commons.command.execution.WrappedCommandExecutor;
import club.abelix.core.velocity.gamer.Gamer;
import com.velocitypowered.api.proxy.Player;
import lombok.Getter;

@Getter
public class CommandExecutionSession extends ExecutionSession {

    private final String prefix;

    private final ProxiedCommandSender sender;
    private final Gamer gamer;

    private final Player player;

    public CommandExecutionSession(String prefix, WrappedCommandExecutor executor,
                                   ProxiedCommandSender sender, Gamer gamer, String[] arguments) {
        super(executor, arguments);

        this.prefix = prefix;
        this.sender = sender;
        this.gamer = gamer;
        this.player = gamer == null ? null : gamer.getPlayer();
    }

    @Override
    public void sendPrefixedMessage(String message) {
        this.sendMessage(prefix + message);
    }

    @Override
    public void sync(Runnable runnable) {
        runnable.run();
    }
}
