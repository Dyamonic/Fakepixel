package club.abelix.core.app.command;

import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.core.app.api.command.CoreExecutionSession;

@NamedCommand("version")
@WithAlias("ver")
@IgnoreLogs
@WithAlias("bukkit:ver")
public final class VersionCommand {

    @Executor
    public static void execute(CoreExecutionSession session) throws CommandTranslationException {
        session.sendLocalizedMessage("core_bukkit_version");
    }

}
