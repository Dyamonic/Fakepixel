package club.abelix.api.base.command;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.ExecutionSession;
import club.abelix.commons.command.execution.WrappedCommandExecutor;
import lombok.Getter;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.function.Consumer;

@Getter
public class BukkitExecutionSession extends ExecutionSession {

    private final String prefix;

    private final ProxiedCommandSender sender;
    private final Gamer gamer;

    private final Player player;

    public BukkitExecutionSession(String prefix, WrappedCommandExecutor executor,
                                  ProxiedCommandSender sender, Gamer gamer, String[] arguments) {
        super(executor, arguments);

        this.prefix = prefix;
        this.sender = sender;
        this.gamer = gamer;
        this.player = gamer == null ? null : gamer.getPlayer();
    }

    @Override
    public void sendPrefixedMessage(@Nullable String message) {
        this.sendMessage(prefix + message);
    }

    public Gamer getOfflineGamer(int argument, Consumer<Gamer> gamerConsumer) throws CommandTranslationException {
        Gamer gamer = Gamer.getOrCreate(getArgument(argument), gamerConsumer);

        if (gamer == null) {
            throw new CommandTranslationException("player_not_found");
        }

        return gamer;
    }

    public Gamer getGamer(int argument) throws CommandTranslationException {
        Gamer gamer = Gamer.getGamer(getArgument(argument));

        if (gamer == null) {
            throw new CommandTranslationException("player_offline");
        }

        return gamer;
    }

    public Player getPlayer(int argument) throws CommandTranslationException {
        return getGamer(argument).getPlayer();
    }

    @Override
    public void sync(Runnable runnable) {
        runnable.run();
    }
}
