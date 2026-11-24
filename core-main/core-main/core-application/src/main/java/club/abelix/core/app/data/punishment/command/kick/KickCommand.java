package club.abelix.core.app.data.punishment.command.kick;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.dependency.Dependency;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.punishment.PunishmentManager;

import java.io.IOException;

@NamedCommand("kick")
public final class KickCommand {

    @Dependency
    private PunishmentManager manager;

    @Executor
    @Tabcomplete("[playerName]")
    @WithCondition("minArgs: 2")
    @WithPermission("command.kick")
    public void execute(final CoreExecutionSession session) throws CommandTranslationException, IOException {
        manager.processKick(
                session.getSender(), session.getArgument(0),
                session.getArgumentString(1)
        );
    }
}
