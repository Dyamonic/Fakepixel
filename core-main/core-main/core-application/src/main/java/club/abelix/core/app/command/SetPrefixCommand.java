package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.execution.SubcommandExecutor;
import club.abelix.commons.data.prefix.PrefixSqlHandler;
import club.abelix.commons.data.rank.Rank;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.regex.Patterns;
import club.abelix.commons.util.time.Cooldown;
import club.abelix.commons.util.time.Duration;
import club.abelix.commons.util.word.BadWordUtil;
import club.abelix.core.app.CoreApplication;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.gamer.Gamer;
import lombok.NonNull;
import lombok.val;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@NamedCommand("setprefix")
@WithAlias("prefix")
public final class SetPrefixCommand {

    private static final List<String> BLOCKED_GROUPS = CoreApplication.getInstance()
            .getRanksService()
            .findAllRanks()
            .stream()
            .map(Rank::getName)
            .map(String::toLowerCase)
            .toList();

    private static final String PREFIX_COOLDOWN = "CustomPrefix";
    private static final String[] FORBIDDEN_PREFIXES = new String[]{
            "owner", "admin", "mod", "moderator", "helper", "jrhelper", "co-owner", "deputy",
            "bt", "beta", "sponsor", "dev", "developer", "watchdog", "deputy", "build", "youtube",
            "curator", "tech", "gm", "ceo", "founder", "enthusiast", "game master", "master", "skyblock",
            "manager", "team", "op", "moder", "staff", "team"
    };

    @Executor
    @WithPermission("command.setprefix")
    @WithCondition("minArgs: 1")
    @WithCondition("disableStaff")
    public static void execution(CoreExecutionSession session) throws CommandTranslationException {
        Gamer gamer = session.getGamer();
        Language language = gamer.getLanguage();

        String prefix = session.getArgumentString(0);

        if (BLOCKED_GROUPS.contains(prefix.toLowerCase())) {
            gamer.sendLocalizedMessage("command_not_access");
            return;
        }

        // Проверяем кулдаун
        if (gamer.getCooldown(PREFIX_COOLDOWN).isActive() && !gamer.hasPermission("command.setprefix.cooldownbypass")) {
            Cooldown cooldown = gamer.getCooldown(PREFIX_COOLDOWN);

            gamer.sendLocalizedMessage("setprefix_cooldown",
                    "%duration%", Duration.durationWordsMs(language, cooldown.getAvailableAt()));
            return;
        }

        if (!Patterns.isEnglishLetters(prefix)) {
            throw new CommandTranslationException("setprefix_invalid_prefix");
        }

        // Форматируем префикс
        String formattedPrefix = prefixFormat(prefix);

        if (formattedPrefix == null) {
            throw new CommandTranslationException("setprefix_invalid_prefix");
        }

        if (BadWordUtil.isSwearWord(formattedPrefix, language)) {
            throw new CommandTranslationException("setprefix_invalid_prefix");
        }

        if (isForbidden(prefix)) {
            throw new CommandTranslationException("setprefix_invalid_prefix");
        }

        PrefixSqlHandler.INSTANCE.setPrefix(gamer.getPlayerId(), formattedPrefix, LocalDateTime.now().plusYears(1));
        gamer.sendLocalizedMessage("setprefix_successfully", "%prefix%", formattedPrefix);

        if (gamer.hasPermission("command.setprefix.cooldownbypass")) return;

        gamer.setCooldown(PREFIX_COOLDOWN, 14, TimeUnit.DAYS);
    }

    @SubcommandExecutor("reset")
    public static void reset(CoreExecutionSession session) throws CommandTranslationException {
        Gamer gamer;
        if (session.getArgumentCount() == 1) {
            if (!session.getRanks().hasPermission("prefix.other.reset")) {
                session.sendMessage("Insufficient permission!");
                return;
            }

            gamer = session.getGamer(0);
        } else {
            gamer = session.getGamer();
        }

        if (gamer.getPrefix() != null) {
            PrefixSqlHandler.INSTANCE.removePrefix(gamer.getPlayerId());
            session.sendLocalizedMessage("setprefix_reset");
        }
    }

    private static String prefixFormat(@NonNull String prefix) {
        if (prefix.length() > 8) {
            return null;
        }

        return "§l" + removeSpecialCharacters(prefix).toUpperCase() + " ";
    }

    private static String removeSpecialCharacters(String input) {
        String pattern = "[^a-zA-Z0-9]";
        return input.replaceAll(pattern, "");
    }

    private static boolean isForbidden(@NonNull String input) {
        return Arrays.stream(FORBIDDEN_PREFIXES)
                .filter(prefix -> input.toLowerCase().contains(prefix))
                .findAny()
                .orElse(null) != null;
    }
}
