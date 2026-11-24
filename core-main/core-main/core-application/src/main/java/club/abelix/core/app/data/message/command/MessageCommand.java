package club.abelix.core.app.data.message.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.data.message.MessageManager;

import java.util.concurrent.TimeUnit;

@WithAlias("w")
@WithAlias("whisper")
@WithAlias("msg")
@WithAlias("m")
@WithAlias("tell")
@WithAlias("text")
@WithAlias("t")
@WithAlias("pm")
@NamedCommand("message")
public final class MessageCommand {

    private static final long MIN_PLAY_TIME = TimeUnit.HOURS.toSeconds(2);

    @Executor
    @Tabcomplete("[playerName]")
    @WithCondition("notBanned")
    @WithCondition("notMuted")
    @WithCondition("minArgs: 2")
    public static void execute(CoreExecutionSession session) throws CommandTranslationException {
        Gamer sender = session.getGamer();
        Gamer receiver = session.getGamer(0);

        if (sender.getGlobalTime() < MIN_PLAY_TIME) {
            sender.sendMessage("§cYou must have at least §f2 hours §cof playtime before you can send private messages.");
            return;
        }

        if (!receiver.isHelper() && !sender.checkVanished()) {
            return;
        }

        if (receiver == sender) {
            throw new CommandTranslationException("message_self");
        }

        MessageManager.INSTANCE.processMessage(sender, receiver, session.getArgumentString(1));
    }

}
