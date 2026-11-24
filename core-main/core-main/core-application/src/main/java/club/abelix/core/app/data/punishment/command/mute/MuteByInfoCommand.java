package club.abelix.core.app.data.punishment.command.mute;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.dependency.Dependency;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.data.punishment.meta.MuteMeta;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.punishment.PunishmentManager;

import java.util.List;
import java.util.stream.Collectors;

@NamedCommand("mutebyinfo")
public final class MuteByInfoCommand {

    @Dependency
    private PunishmentManager manager;

    @Executor
    @WithCondition("minArgs: 1")
    @WithCondition("disableConsole")
    @WithPermission("command.mutebyinfo")
    public void execute(CoreExecutionSession session) throws CommandTranslationException {
        if (session.getArgumentCount() == 0)
            throw new CommandTranslationException("mutebyinfo_unknown_usage");

        String banBy = session.getArgument(0);

        List<MuteMeta> mutes = manager.getMuteMetas().stream()
                .filter(muteMeta -> muteMeta.getAdminName().equalsIgnoreCase(banBy))
                .collect(Collectors.toList());

        if (mutes.isEmpty()) {
            throw new CommandTranslationException("mutebyinfo_empty_result", "%ban_by%", banBy);
        }

        session.sendLocalizedMessage("mutebyinfo_result", "%ban_by%", banBy);

        for (MuteMeta muteMeta : mutes) {
            session.sendMessage(manager.getMuteJsonText(muteMeta, session.getLanguage(), true));
        }
    }
}
