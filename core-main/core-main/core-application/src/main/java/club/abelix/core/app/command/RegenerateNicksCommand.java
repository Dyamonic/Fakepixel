package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.data.nickname.NickManager;
import lombok.With;

@NamedCommand("regennicks")
public class RegenerateNicksCommand {

    @Executor
    @WithPermission("command.nicks.regenerate")
    public static void execute(CoreExecutionSession session) throws CommandTranslationException {
        Gamer gamer = session.getGamer();

        if (session.getArgumentCount() > 0) {
            gamer = session.getGamer(0);
        }

        session.sendPrefixedMessage("Nicks regenerated for " + gamer.getDisplayName());

        NickManager.IMP.clearNicknameCache(gamer);
    }
}
