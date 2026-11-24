package club.abelix.core.app.command;

import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.exception.CommandException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.util.color.ChatColor;
import club.abelix.core.app.CoreApplication;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.util.BuildInfo;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: core</p>
 * <p>CoreVersionCommand creation on 06.06.2023 at 02:55</p>
 */
@WithAlias("corev")
@WithAlias("corv")
@WithAlias("corver")
@WithAlias("corever")
@IgnoreLogs
@NamedCommand("coreversion")
public class CoreVersionCommand {

    @Executor
    public static void execute(CoreExecutionSession session) throws CommandException {
        session.sendMessage(ChatColor.AQUA + BuildInfo.getVersion());
    }
}
