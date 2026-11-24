package club.abelix.core.app.command;

import club.abelix.chatfilter.proto.TestFilterResponse;
import club.abelix.commons.chatfilter.ChatFilterGrpcIntegration;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.execution.Executor;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.gamer.Gamer;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.stream.IntStream;

@NamedCommand("tch")
@WithAlias("testchat")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public final class TestChatCommand {

    ChatFilterGrpcIntegration chatFilterGrpc;

    @Executor
    @WithPermission("command.testchat")
    public void execute(CoreExecutionSession session) {
        if (session.getArgumentCount() == 0) {
            session.sendMessage("§cИспользование: §6/tch <сообщение>");
            return;
        }

        Gamer gamer = session.getGamer();
        if (gamer == null) {
            session.sendMessage("§cТолько для игроков");
            return;
        }

        String message = IntStream.range(0, session.getArgumentCount())
                .mapToObj(session::getArgument)
                .reduce((a, b) -> a + " " + b)
                .orElse("");

        session.sendMessage("§7Тестируем чат-фильтр для: §f" + message);

        chatFilterGrpc.testFilter(gamer.getName(), message)
                .thenAccept(response -> displayResult(session, response))
                .exceptionally(t -> {
                    session.sendMessage("§cОшибка подключения к чат-фильтру: " + t.getMessage());
                    return null;
                });
    }

    private void displayResult(CoreExecutionSession session, TestFilterResponse r) {
        session.sendMessage("§8§m                                        ");
        session.sendMessage("§6Chat Filter Test");
        session.sendMessage("  §7Trust:  " + trustColor(r.getTrustLevel()) + r.getTrustLevel());
        session.sendMessage("  §7Фильтр: §f" + r.getSource());

        if ("NONE".equals(r.getAction())) {
            session.sendMessage("  §7Вердикт: §aЧИСТО ✓");
        } else {
            String actionColor = "BAN".equals(r.getAction()) ? "§c" : "§e";
            session.sendMessage("  §7Вердикт: " + actionColor + r.getAction());
            if (!r.getCategory().isEmpty()) {
                session.sendMessage("  §7Категория: §f" + r.getCategory());
            }
            if (!r.getReason().isEmpty()) {
                session.sendMessage("  §7Причина: §f" + r.getReason());
            }
            if (r.getDurationMs() == -1) {
                session.sendMessage("  §7Срок: §cПостоянно");
            } else if (r.getDurationMs() > 0) {
                session.sendMessage("  §7Срок: §f" + formatDuration(r.getDurationMs()));
            }
            if (!r.getMatchedPattern().isEmpty()) {
                session.sendMessage("  §7Паттерн: §f" + r.getMatchedPattern());
                session.sendMessage("  §7Язык: §f" + r.getLanguage());
            }
            if (r.getScore() > 0 && !"DICTIONARY".equals(r.getSource())) {
                session.sendMessage("  §7Скор: §f" + String.format("%.3f", r.getScore()));
            }
        }

        if (!r.getTopScores().isEmpty()) {
            session.sendMessage("  §7Скоры: §8" + r.getTopScores());
        }
        if (!r.getNote().isEmpty()) {
            session.sendMessage("  §8" + r.getNote());
        }
        session.sendMessage("§8§m                                        ");
    }

    private String trustColor(String level) {
        return switch (level) {
            case "VERIFIED" -> "§a";
            case "HIGH"     -> "§2";
            case "MEDIUM"   -> "§e";
            case "LOW"      -> "§6";
            default         -> "§c"; // UNTRUSTED
        };
    }

    private String formatDuration(long ms) {
        long s = ms / 1000;
        if (s < 60)   return s + "с";
        long m = s / 60;
        if (m < 60)   return m + "м";
        long h = m / 60;
        if (h < 24)   return h + "ч";
        return (h / 24) + "д";
    }
}
