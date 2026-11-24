package club.abelix.core.app.data.message.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.execution.Executor;
import club.abelix.core.app.api.command.CoreExecutionSession;

@WithAlias("ss")
@NamedCommand("socialspy")
public final class SocialSpyCommand {

    @Executor
    @WithCondition("disableConsole")
    @WithPermission("command.socialspy")
    public static void execute(CoreExecutionSession session) {
        boolean spyStatus = !(session.getProperty("SocialSpy") == 1);

        session.sendLocalizedMessage("socialspy_" + (spyStatus ? "enabled" : "disabled"));
        session.setProperty("SocialSpy", spyStatus ? 1 : 0);
    }
}
