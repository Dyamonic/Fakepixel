package club.abelix.core.app.command;

import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.execution.SubcommandExecutor;
import club.abelix.core.app.api.command.CoreExecutionSession;

@IgnoreLogs
@NamedCommand("help")
public final class HelpCommand {

    @Executor
    public static void execute(CoreExecutionSession session) {
        session.sendLocalizedMessage("core_help");
    }

    @SubcommandExecutor("commands")
    public static void commands(CoreExecutionSession session) {
        session.sendLocalizedMessage("core_help_commands");
    }
}