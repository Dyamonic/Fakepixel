package club.abelix.api.base.joinmessage.custom;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.execution.Executor;

@NamedCommand("createjoinmessage")
@WithAlias("createjm")
public class JoinMessageCreateCommand {

    @WithCondition("disableConsole")
    @Executor
    public void create(BukkitExecutionSession session) {

    }
}
