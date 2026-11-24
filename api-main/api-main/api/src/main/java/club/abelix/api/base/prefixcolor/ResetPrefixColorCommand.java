package club.abelix.api.base.prefixcolor;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;

@NamedCommand("prefixcolorreset")
@WithAlias("prefixcolorsreset")
@WithAlias("colorreset")
@WithAlias("colorsreset")
public class ResetPrefixColorCommand {
    @Executor
    @WithPermission("prefix.colored")
    @WithCondition("disableConsole")
    public static void execute(BukkitExecutionSession session) throws CommandTranslationException {
        session.getGamer().changePrefixColor(null);
        session.sendLocalizedMessage("prefix_color_reset");
    }
}
