package club.abelix.core.app.data.punishment.command.mute;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.dependency.Dependency;
import club.abelix.commons.command.exception.CommandException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.punishment.PunishmentManager;

@NamedCommand("unmute")
public final class UnMuteCommand {

    @Dependency
    private PunishmentManager manager;

    @Executor
    @Tabcomplete("[playerName]")
    @WithCondition("minArgs: 1")
    @WithPermission("command.unmute")
    public void execute(CoreExecutionSession session) throws CommandException {
        manager.processUnmute(session.getSender(), session.getArgument(0));
    }

}
