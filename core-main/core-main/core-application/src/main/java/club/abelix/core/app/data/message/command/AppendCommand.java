package club.abelix.core.app.data.message.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.data.message.MessageManager;

@WithAlias("a")
@NamedCommand("append")
public final class AppendCommand {

    @Executor
    @WithCondition("minArgs: 1")
    @WithCondition("notBanned")
    @WithCondition("notMuted")
    public static void execute(CoreExecutionSession session) throws CommandTranslationException {
        Gamer sender = session.getGamer();
        Gamer gamer = MessageManager.INSTANCE.getLastFrom(sender);

        if (gamer == null || !gamer.isOnline() || (gamer.isVanished() && !sender.canSeeVanish())) {
            throw new CommandTranslationException("append_nobody_pm");
        }

        if (!gamer.isHelper() && !sender.checkVanished()) {
            return;
        }

        MessageManager.INSTANCE.processMessage(sender, gamer, session.getArgumentString());
    }
}
