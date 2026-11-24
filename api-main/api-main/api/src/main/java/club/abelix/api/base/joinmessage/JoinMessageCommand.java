package club.abelix.api.base.joinmessage;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.execution.Executor;

@WithAlias("joinmessages")
@WithAlias("jm")
@WithAlias("joinmsg")
@NamedCommand("joinmessage")
@IgnoreLogs
public class JoinMessageCommand {

    @Executor
    @WithCondition("disableConsole")
    public static void executor(BukkitExecutionSession session) {
        new JoinMessageChangeGui(null, session.getPlayer());
    }

}
