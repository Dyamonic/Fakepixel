package club.abelix.core.app.data.message.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.data.message.MessageManager;

@WithAlias("r")
@NamedCommand("reply")
public final class ReplyCommand {

    @Executor
    @WithCondition("minArgs: 1")
    @WithCondition("notBanned")
    @WithCondition("notMuted")
    public static void execute(CoreExecutionSession session) throws CommandTranslationException {
        Gamer receiver = session.getGamer();
        Gamer gamer = MessageManager.INSTANCE.getLastTo(receiver);

        if (gamer == null || !gamer.isOnline() || gamer.isVanished() && !receiver.canSeeVanish()) {
            throw new CommandTranslationException("reply_nobody_pm");
        }

        if (!gamer.isHelper() && !receiver.checkVanished()) {
            return;
        }

        MessageManager.INSTANCE.processReply(receiver, gamer, session.getArgumentString());
    }
}
