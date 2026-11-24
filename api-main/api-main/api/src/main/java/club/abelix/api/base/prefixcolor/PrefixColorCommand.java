package club.abelix.api.base.prefixcolor;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;

@NamedCommand("prefixcolor")
@WithAlias("prefixcolors")
@WithAlias("color")
@WithAlias("colors")
@IgnoreLogs
public class PrefixColorCommand {

    @Executor
    @WithCondition("disableConsole")
    public static void execute(BukkitExecutionSession session) throws CommandTranslationException {
        new PrefixColorGui(null, session.getGamer());
    }
}
