package club.abelix.core.app.data.punishment.command.mute;

import club.abelix.commons.command.Async;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.dependency.Dependency;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.data.punishment.meta.MuteMeta;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.regex.Patterns;
import club.abelix.commons.util.time.Duration;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.punishment.PunishmentManager;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@NamedCommand("mutelist")
public final class MuteListCommand {

    @Dependency
    private PunishmentManager manager;

    @Async
    @Executor
    @WithPermission("command.mute.list")
    @WithCondition("disableConsole")
    public void execute(CoreExecutionSession session) throws CommandTranslationException {
        long time = TimeUnit.HOURS.toMillis(6);

        if (session.getArgumentCount() > 0) {
            try {
                time = Patterns.parseTime(session.getArgument(0), TimeUnit.MILLISECONDS);
            } catch (IllegalArgumentException e) {
                throw new CommandTranslationException("core_invalid_time_format");
            }
        }

        Language language = session.getLanguage();

        long startDate = System.currentTimeMillis() - time;

        List<MuteMeta> mutes = manager.getMuteMetas().stream()
                .filter(muteMeta -> muteMeta.getIssueDate() >= startDate)
                .sorted((mute1, mute2) -> (int) (mute1.getIssueDate() - mute2.getIssueDate()))
                .collect(Collectors.toList());

        String durationString = Duration.durationWordsMs(language, time);

        if (mutes.isEmpty()) {
            throw new CommandTranslationException("mutelist_empty_result", "%duration%", durationString);
        }

        session.sendLocalizedMessage("mutelist_result", "%duration%", durationString);

        for (MuteMeta mute : mutes) {
            session.sendMessage(manager.getMuteJsonText(mute, language, session.getGamer().isHelper()));
        }
    }

}
