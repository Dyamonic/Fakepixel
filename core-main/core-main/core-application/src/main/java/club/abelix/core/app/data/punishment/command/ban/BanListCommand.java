package club.abelix.core.app.data.punishment.command.ban;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.dependency.Dependency;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.data.punishment.meta.BanMeta;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.regex.Patterns;
import club.abelix.commons.util.time.Duration;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.punishment.PunishmentManager;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@NamedCommand("banlist")
public final class BanListCommand {

    @Dependency
    private PunishmentManager manager;

    @Executor
    @WithPermission("command.banlist")
    @WithCondition("disableConsole")
    public void execute(CoreExecutionSession session) throws CommandTranslationException {
        boolean full = session.getGamer().hasPermission("command.banlist");

        long time = TimeUnit.DAYS.toMillis(1);

        if (session.getArgumentCount() > 0) {
            try {
                time = Patterns.parseTime(session.getArgument(0), TimeUnit.MILLISECONDS);
            } catch (IllegalArgumentException e) {
                throw new CommandTranslationException("core_invalid_time_format");
            }
        }

        Language language = session.getLanguage();

        long startDate = System.currentTimeMillis() - time;

        List<BanMeta> bans = manager.getBanMetas().stream()
                .filter(banMeta -> banMeta.getIssueDate() >= startDate)
                .sorted((ban1, ban2) -> (int) (ban1.getIssueDate() - ban2.getIssueDate()))
                .collect(Collectors.toList());

        String durationString = Duration.durationWordsMs(language, time);

        if (bans.isEmpty()) {
            throw new CommandTranslationException("banlist_empty_result", "%duration%", durationString);
        }

        session.sendLocalizedMessage("banlist_result", "%duration%", durationString);

        for (BanMeta ban : bans) {
            session.sendMessage(manager.getBanJsonText(ban, language, session.getGamer().isHelper(), full));
        }
    }

}
