package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.execution.SubcommandExecutor;
import club.abelix.commons.command.tabcomplete.AbstractTabcompleter;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.commons.command.tabcomplete.TabcompleteManager;
import club.abelix.commons.data.trust.TrustFactor;
import club.abelix.commons.data.trust.TrustLevel;
import club.abelix.commons.data.trust.service.TrustService;
import club.abelix.commons.gamer.ICommandSender;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.gamer.Gamer;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@NamedCommand("trust")
@WithAlias("tf")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public final class TrustCommand {

    static {
        TabcompleteManager.IMP.addCompleter("trustLevel", new AbstractTabcompleter() {
            @Override
            public List<String> complete(ICommandSender sender, String label) {
                return any(label, Arrays.stream(TrustLevel.values()).map(Enum::name));
            }
        });
    }

    TrustService trustService;

    // /trust [player]
    @Executor
    @WithPermission("command.trust")
    public void execute(CoreExecutionSession session) {
        Gamer self = session.getGamer();

        if (session.getArgumentCount() == 0) {
            if (self == null) {
                sendUsage(session);
                return;
            }
            showTrust(session, self);
            return;
        }

        resolveGamer(session, session.getArgument(0), target -> showTrust(session, target));
    }

    // /trust recalculate|recalc <player>
    @SubcommandExecutor("recalculate")
    @WithAlias("recalc")
    @Tabcomplete("[playerName]")
    @WithPermission("command.trust.recalculate")
    public void doRecalculate(CoreExecutionSession session) throws CommandTranslationException {
        if (session.getArgumentCount() == 0) {
            session.sendMessage("§cИспользование: §6/trust recalculate <игрок>");
            return;
        }

        val target = session.getOfflineGamer(0, g -> g.loadPlayerId());
        session.sendMessage("§eПересчет trust фактора для §6" + target.getName() + "§e...");

        trustService.recalculateTrustFactor(target.getPlayerId())
            .thenAccept(tf -> displayTrustFactor(session, target, tf))
            .exceptionally(t -> { session.sendMessage("§cОшибка при пересчете: " + t.getMessage()); return null; });
    }

    // /trust set <player> <level>
    @SubcommandExecutor("set")
    @Tabcomplete("[playerName] [trustLevel]")
    @WithPermission("command.trust.set")
    public void doSet(CoreExecutionSession session) throws CommandTranslationException {
        if (session.getArgumentCount() < 2) {
            sendSetUsage(session);
            return;
        }

        TrustLevel level;
        try {
            level = TrustLevel.valueOf(session.getArgument(1).toUpperCase());
        } catch (IllegalArgumentException e) {
            session.sendMessage("§cНеверный уровень: §6" + session.getArgument(1));
            sendSetUsage(session);
            return;
        }

        val target = session.getOfflineGamer(0, g -> g.loadPlayerId());
        trustService.setForcedLevel(target.getPlayerId(), level)
            .thenAccept(tf -> session.sendMessage(
                "§aПринудительный уровень §6" + level.name() + " §aвыставлен для §6" + target.getName()))
            .exceptionally(t -> { session.sendMessage("§cОшибка: " + t.getMessage()); return null; });
    }

    // /trust clear <player>
    @SubcommandExecutor("clear")
    @Tabcomplete("[playerName]")
    @WithPermission("command.trust.clear")
    public void doClear(CoreExecutionSession session) throws CommandTranslationException {
        if (session.getArgumentCount() == 0) {
            session.sendMessage("§cИспользование: §6/trust clear <игрок>");
            return;
        }

        val target = session.getOfflineGamer(0, g -> g.loadPlayerId());
        trustService.clearForcedLevel(target.getPlayerId())
            .thenAccept(tf -> {
                session.sendMessage("§aПринудительный уровень сброшен для §6" + target.getName());
                displayTrustFactor(session, target, tf);
            })
            .exceptionally(t -> { session.sendMessage("§cОшибка: " + t.getMessage()); return null; });
    }

    private void showTrust(CoreExecutionSession session, Gamer target) {
        trustService.getTrustFactor(target.getPlayerId())
            .thenAccept(opt -> {
                if (opt.isPresent()) {
                    displayTrustFactor(session, target, opt.get());
                } else {
                    session.sendMessage("§cTrust фактор не найден для §6" + target.getName());
                    session.sendMessage("§eИспользуйте §6/trust recalculate " + target.getName() + " §eдля расчета");
                }
            })
            .exceptionally(t -> { session.sendMessage("§cОшибка при загрузке: " + t.getMessage()); return null; });
    }

    private void sendUsage(CoreExecutionSession session) {
        session.sendMessage("§8§m                                                ");
        session.sendMessage("§6/trust §8— §eуправление trust factor");
        session.sendMessage("  §6/trust §8[игрок] §7— посмотреть trust");
        session.sendMessage("  §6/trust recalculate §8<игрок> §7— пересчитать");
        session.sendMessage("  §6/trust set §8<игрок> <уровень> §7— принудительный уровень");
        session.sendMessage("  §6/trust clear §8<игрок> §7— сбросить принудительный уровень");
        session.sendMessage("  §6/trust top §7— распределение по уровням");
        session.sendMessage("§8§m                                                ");
    }

    // /trust help
    @SubcommandExecutor("help")
    @WithPermission("command.trust")
    public void doHelp(CoreExecutionSession session) {
        sendUsage(session);
    }

    // /trust top — распределение игроков по уровням
    @SubcommandExecutor("top")
    @WithPermission("command.trust")
    @SuppressWarnings("unchecked")
    public void doTop(CoreExecutionSession session) {
        TrustLevel[] levels = TrustLevel.values();

        CompletableFuture<Map.Entry<TrustLevel, Integer>>[] futures = Arrays.stream(levels)
            .map(lvl -> trustService.countByLevel(lvl).thenApply(count -> Map.entry(lvl, count)))
            .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures)
            .thenAccept(v -> {
                session.sendMessage("§8§m                                                ");
                session.sendMessage("§6Trust Factor: §eРаспределение игроков");
                session.sendMessage("");
                for (var f : futures) {
                    val entry = f.join();
                    session.sendMessage("  " + levelColor(entry.getKey()) + entry.getKey().name() +
                        " §8— §f" + entry.getValue() + " §7игроков");
                }
                session.sendMessage("§8§m                                                ");
            })
            .exceptionally(t -> { session.sendMessage("§cОшибка: " + t.getMessage()); return null; });
    }

    private void resolveGamer(CoreExecutionSession session, String targetName, Consumer<Gamer> action) {
        Gamer target = Gamer.getGamer(targetName);
        if (target != null) {
            action.accept(target);
            return;
        }
        Gamer.getOfflineGamer(targetName, offlineGamer -> {
            if (offlineGamer == null) {
                session.sendMessage("§cИгрок §6" + targetName + " §cне найден!");
                return;
            }
            action.accept(offlineGamer);
        });
    }

    private void displayTrustFactor(CoreExecutionSession session, Gamer target, TrustFactor tf) {
        session.sendMessage("§8§m                                                ");
        session.sendMessage("§6Trust Factor: §e" + target.getName());
        session.sendMessage("");

        TrustLevel displayLevel = tf.isForcedLevel() ? tf.getForcedLevel() : tf.getLevel();
        String levelDisplay = levelColor(displayLevel) + displayLevel.name();
        if (tf.isForcedLevel()) {
            levelDisplay += " §8(принудительно, нативный: " + levelColor(tf.getLevel()) + tf.getLevel().name() + "§8)";
        }

        session.sendMessage("  §7Уровень: " + levelDisplay + " §8(" + tf.getScore() + " pts)");
        if (!tf.isStaff()) {
            session.sendMessage("  §7Донат: " + (tf.isHasDonation() ? "§aЕсть" : "§cНет") +
                    " §8(" + tf.getDonationEffort() + " pts)");
        }
        session.sendMessage("  §7Наиграно: §f" + String.format("%.1f", tf.getPlaytimeHours()) + "§7ч");
        session.sendMessage("  §7Муты: §f" + tf.getMutesCount() + " §8| §7Баны: §f" + tf.getBansCount());
        session.sendMessage("  §7Проверяется GPT: " + (displayLevel.isRequiresModeration() ? "§aДа" : "§aНет"));
        session.sendMessage("  §7Обновлено: §f" + tf.getLastRecalculated());
        session.sendMessage("§8§m                                                ");
    }

    private void sendSetUsage(CoreExecutionSession session) {
        session.sendMessage("§cИспользование: §6/trust set <игрок> <уровень>");
        session.sendMessage("§7Уровни: §f" + Arrays.stream(TrustLevel.values())
            .map(Enum::name).collect(Collectors.joining("§8, §f")));
    }

    private String levelColor(TrustLevel level) {
        return switch (level) {
            case VERIFIED -> "§a";
            case HIGH -> "§2";
            case MEDIUM -> "§e";
            case LOW -> "§6";
            case UNTRUSTED -> "§c";
        };
    }
}
