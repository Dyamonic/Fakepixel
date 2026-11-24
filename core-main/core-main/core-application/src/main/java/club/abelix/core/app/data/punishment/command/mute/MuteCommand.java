package club.abelix.core.app.data.punishment.command.mute;

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

@NamedCommand("mute")
public final class MuteCommand {

    @Dependency
    private PunishmentManager manager;

    @Executor
    @Tabcomplete("[playerName] [any: 10m|15m|30m|60m|5d] [any: Advertising|Trolling|Discrimination|Spam|Personal Tip Requests|Verbal abuse]")
    @WithCondition("minArgs: 3")
    @WithPermission("command.mute")
    public void execute(CoreExecutionSession session) throws CommandTranslationException, IOException {
        manager.processMute(
                session.getSender(), session.getArgument(0),
                session.getArgumentString(2), session.getArgument(1)
        );
    }


}
