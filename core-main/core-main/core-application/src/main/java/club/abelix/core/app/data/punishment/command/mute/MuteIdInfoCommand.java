package club.abelix.core.app.data.punishment.command.mute;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.dependency.Dependency;
import club.abelix.commons.command.exception.CommandException;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.data.punishment.meta.MuteMeta;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.punishment.PunishmentManager;

import java.util.Optional;

@NamedCommand("muteidinfo")
public final class MuteIdInfoCommand {

    @Dependency
    private PunishmentManager manager;

    @Executor
    @WithPermission("command.muteidinfo")
    @WithCondition("disableConsole")
    @WithCondition("minArgs: 1")
    public void execute(CoreExecutionSession session) throws CommandException {
        int id = session.getIntArgument(0, "ID мута");

        Optional<MuteMeta> muteMeta = manager.findMuteMeta(id);

        if (!muteMeta.isPresent()) {
            throw new CommandTranslationException("muteidinfo_empty_result");
        }

        session.sendLocalizedMessage("muteidinfo_result", "%id%", id);
        session.sendMessage(manager.getMuteJsonText(muteMeta.get(), session.getLanguage(), true));
    }
}
