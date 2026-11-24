package club.abelix.core.app.command;

import club.abelix.commons.chatfilter.ChatFilterGrpcIntegration;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.tabcomplete.AbstractTabcompleter;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.commons.command.tabcomplete.TabcompleteManager;
import club.abelix.commons.gamer.ICommandSender;
import club.abelix.commons.util.json.JSON;
import club.abelix.core.app.api.command.CoreExecutionSession;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;


@WithAlias("clog")
@WithAlias("срфедщп")
@NamedCommand("chatlog")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public final class ChatLogCommand {

    static {
        TabcompleteManager.IMP.addCompleter("chatlogDuration", new AbstractTabcompleter() {
            @Override
            public List<String> complete(ICommandSender sender, String label) {
                return any(label, "30m", "1h", "6h", "12h", "1d", "7d");
            }
        });
    }

    ChatFilterGrpcIntegration chatFilterGrpc;

    @Executor
    @Tabcomplete("[playerName] [chatlogDuration]")
    @WithPermission("command.chatlog")
    public void execute(CoreExecutionSession session) {
        if (session.getArgumentCount() < 2) {
            session.sendMessage("§cИспользование: §6/chatlog <ник> §e<30m|1h|6h|12h|1d|7d>");
            return;
        }

        String playerName = session.getArgument(0);
        String durationStr = session.getArgument(1);

        long durationMs = parseDuration(durationStr);
        if (durationMs <= 0) {
            session.sendMessage("§cНеверный формат времени. Используй: 30m, 1h, 6h, 12h, 1d, 7d");
            return;
        }

        long fromMs = System.currentTimeMillis() - durationMs;
        long start = System.currentTimeMillis();

        session.sendMessage("§7Загружаю логи чата для §e" + playerName
                + " §7за последние §e" + durationStr + "§7...");

        chatFilterGrpc.getChatLogs(playerName, fromMs)
                .whenComplete((response, throwable) -> {
                    if (throwable != null) {
                        session.sendMessage("§cОшибка при запросе логов: " + throwable.getMessage());
                        return;
                    }

                    if (!response.getError().isEmpty()) {
                        session.sendMessage("§cОшибка: " + response.getError());
                        return;
                    }

                    long elapsed = System.currentTimeMillis() - start;
                    String url = response.getPasteUrl();
                    int total = response.getTotal();

                    String message = "§7Логи чата §e" + playerName
                            + "§7: §b" + total + " §7сообщений, "
                            + "§8[" + elapsed + "ms] §7→ §a[открыть]";

                    session.sendMessage(String.format(JSON.CLICK_URL_LAYOUT, message, url));
                });
    }

    private static long parseDuration(String s) {
        if (s == null || s.isBlank()) return -1;
        try {
            char unit = s.charAt(s.length() - 1);
            long value = Long.parseLong(s.substring(0, s.length() - 1));
            return switch (unit) {
                case 'd' -> value * 86_400_000L;
                case 'h' -> value * 3_600_000L;
                case 'm' -> value * 60_000L;
                default  -> -1;
            };
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}

