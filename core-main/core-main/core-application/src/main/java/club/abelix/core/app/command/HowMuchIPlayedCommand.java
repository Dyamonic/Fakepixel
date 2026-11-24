package club.abelix.core.app.command;

import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.cooldown.WithCooldown;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.data.session.Session;
import club.abelix.commons.data.session.SessionSqlHandler;
import club.abelix.commons.util.time.Duration;
import club.abelix.commons.util.time.TimeUtil;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.gamer.Gamer;
import lombok.NonNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@IgnoreLogs
@NamedCommand("howmuchiplayed")
@WithAlias("playtime")
public final class HowMuchIPlayedCommand {

    @Executor
    @WithPermission("command.playtime")
    @WithCooldown(value = 15_000, currentServerOnly = false)
    public static void execute(@NonNull final CoreExecutionSession session) {
        Gamer gamer = session.getGamer();

        LocalDate today = LocalDate.now();
        LocalDateTime todayTime = today.atTime(LocalTime.MIDNIGHT);

        SessionSqlHandler.INSTANCE.loadSessions(gamer.getPlayerId(), todayTime).whenComplete((result, cause) -> {
            long totalTime = result.stream()
                    .mapToLong(Session::getElapsedTime)
                    .sum() + gamer.getPlayingSeconds();

            StringBuilder sb = new StringBuilder();
            if (totalTime > 0) {
                sb.append(gamer.getLanguage().getMessage(
                        "played_time_played",
                        "%date%", TimeUtil.getDateString(gamer.getLanguage(), today),
                        "%time%", Duration.durationWordsSec(gamer.getLanguage(), totalTime)
                ));
            } else {
                sb.append(gamer.getLanguage().getMessage("howmuchiplayed_didnt_played"));
            }
            gamer.sendMessage(sb.toString());
        });
    }
}

