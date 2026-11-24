package club.abelix.core.app.command;

import club.abelix.commons.command.Async;
import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.time.Duration;
import club.abelix.commons.util.time.DurationField;
import club.abelix.commons.util.time.DurationFormatter;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.gamer.Gamer;

import java.util.concurrent.TimeUnit;

@IgnoreLogs
@NamedCommand("time")
public final class TimeCommand {

    @Async
    @Executor
    @Tabcomplete("[playerName]")
    public static void execute(CoreExecutionSession session) throws CommandTranslationException {
        Gamer target = session.getGamer();

        if (session.getSender().hasPermission("command.time.bypass") && session.getArgumentCount() > 0) {
            target = session.getOfflineGamer(0, offline -> {
                offline.loadRanks();
                offline.loadPrefix();
                offline.loadProperties();
            });
        }

        if (target == null) {
            session.sendCommandUsage("time");
            return;
        }

        Duration.FormatParams params = new Duration.FormatParams();
        params.locale = session.getLanguage() != Language.RUSSIAN
                ? DurationFormatter.EN_SHORT : DurationFormatter.RU_SHORT;

        params.sourceUnit = TimeUnit.SECONDS;
        params.min = DurationField.SEC;
        params.max = DurationField.MONTH;

        session.sendLocalizedMessage(
                target == session.getGamer() ? "time_you_spent" : "time_other_spent",
                "%name%", target.getDisplayName(),
                "%time%", Duration.format(target.getGlobalTime(), params)
        );
    }

}
