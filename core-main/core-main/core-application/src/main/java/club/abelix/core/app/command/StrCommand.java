package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.language.Language;
import club.abelix.core.app.api.command.CommandSender;
import club.abelix.core.app.api.command.CoreExecutionSession;

import java.time.LocalDateTime;

@NamedCommand("str")
public final class StrCommand {

    @Executor
    @WithPermission("group: SR_MODER")
    public static void doExecution(CoreExecutionSession coreSession) {
        CommandSender sender = coreSession.getSender();
        Language senderLanguage = sender.getLanguage();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDayAgoTime = now.minusDays(7);

//        SessionSqlHandler.INSTANCE.loadSessionsInGroups(Group.HELPER, Group.SR_MODER, sevenDayAgoTime)
//                .thenAccept(sessions -> {
//                    sender.sendLocalizedMessage("work_report_header",
//                            "%from_time%", TimeUtil.getDateTimeString(senderLanguage, sevenDayAgoTime),
//                            "%to_time%", TimeUtil.getDateTimeString(senderLanguage, now)
//                    );
//
//                    sessions.stream()
//                            .collect(Collectors.groupingBy(
//                                    Session::getPlayerId,
//                                    Collectors.summingLong(Session::getElapsedTime)
//                            ))
//                            .entrySet()
//                            .stream()
//                            .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
//                            .forEach(entry -> {
//                                Gamer targetGamer = Gamer.getOfflineGamer(entry.getKey(), gamer -> {
//                                    gamer.loadPlayerId();
//                                    gamer.loadRanks();
//                                    gamer.loadPrefix();
//                                });
//
//                                if (targetGamer == null) return;
//
//                                long elapsedTime = entry.getValue();
//
//                                sender.sendLocalizedMessage("work_report_format",
//                                        "%name%", targetGamer.getDisplayName(),
//                                        "%time%", Duration.durationWordsSec(senderLanguage, elapsedTime)
//                                );
//                            });
//                });

        sender.sendMessage("Temporary disabled");
    }
}
