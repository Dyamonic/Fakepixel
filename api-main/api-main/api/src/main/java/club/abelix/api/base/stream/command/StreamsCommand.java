package club.abelix.api.base.stream.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.base.stream.gui.StreamsGui;
import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.execution.Executor;

@WithAlias("stream")
@WithAlias("стримы")
@WithAlias("трансляции")
@NamedCommand("streams")
@IgnoreLogs
public class StreamsCommand {

    @Executor
    @WithCondition("disableConsole")
    public static void execute(BukkitExecutionSession session) {
        new StreamsGui(session.getPlayer());
    }
}
