package club.abelix.core.app.command;

import club.abelix.commons.command.Async;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandException;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.gamer.Gamer;

@NamedCommand("getname")
@WithAlias("realname")
@WithAlias("rn")
public final class GetNameCommand {

    @Async
    @Executor
    @WithPermission("command.getname")
    @WithCondition("minArgs: 1")
    public static void execute(CoreExecutionSession session) throws CommandException {
        String fakeName = session.getArgumentString(0);

        Gamer gamer = Gamer.getNickedGamer(fakeName);

        if (gamer == null) {
            throw new CommandTranslationException("player_not_found");
        }

        session.sendLocalizedMessage("core_getname_result", "%name%", gamer.getName());
    }

}
