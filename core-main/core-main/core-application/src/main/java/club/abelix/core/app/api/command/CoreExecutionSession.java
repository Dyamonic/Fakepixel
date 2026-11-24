package club.abelix.core.app.api.command;

import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.ExecutionSession;
import club.abelix.commons.command.execution.WrappedCommandExecutor;
import club.abelix.core.app.data.gamer.Gamer;
import lombok.Getter;

import java.util.function.Consumer;

@Getter
public class CoreExecutionSession extends ExecutionSession {

    private final CommandSender sender;
    private final Gamer gamer;

    public CoreExecutionSession(WrappedCommandExecutor executor, CommandSender sender, Gamer gamer, String[] arguments) {
        super(executor, arguments);

        this.sender = sender;
        this.gamer = gamer;
    }

    public Gamer getOfflineGamer(int i, Consumer<Gamer> loader) throws CommandTranslationException {
        Gamer gamer = Gamer.getOfflineGamer(getArgument(i), loader);

        if (gamer == null) {
            throw new CommandTranslationException("player_not_found");
        }

        return gamer;
    }

    public Gamer getGamer(int i) throws CommandTranslationException {
        Gamer gamer = Gamer.getGamer(getArgument(i));

        if (gamer == null || (gamer.isVanished() && !sender.isHelper())) {
            throw new CommandTranslationException("player_offline");
        }

        return gamer;
    }

    @Override
    public void sync(Runnable runnable) {
        runnable.run();
    }
}
