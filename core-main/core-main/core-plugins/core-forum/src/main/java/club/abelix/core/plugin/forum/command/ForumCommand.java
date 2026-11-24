package club.abelix.core.plugin.forum.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.util.password.PasswordHashing;
import club.abelix.commons.util.time.Duration;
import club.abelix.commons.util.time.PlayerCooldownUtil;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.plugin.forum.ForumCorePlugin;
import club.abelix.core.plugin.forum.util.TempMailUtil;
import club.abelix.core.plugin.forum.util.XenApi;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
@NamedCommand("forum")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class ForumCommand {

    private static final Pattern EMAIL_REGEX =
            Pattern.compile("^(?!.*\\.\\.)[A-Za-zА-Яа-я0-9._%+-]+@[A-Za-zА-Яа-я0-9.-]+\\.[A-Za-zА-Яа-я]{2,}$");

    private static final String COOLDOWN_KEY = "forum_register_cooldown";

    ForumCorePlugin coreForumPlugin;
    XenApi xenApi;
    Map<String, Confirmation> confirmationMap = new ConcurrentHashMap<>();

    @Executor
    public void execute(final @NonNull CoreExecutionSession session) {
        val gamer = session.getGamer();
        val language = session.getLanguage();

        if (gamer.isOnline() && !xenApi.canRegister(gamer)) {
            val playedTime = gamer.getGlobalTime();
            val requiredTime = XenApi.MINIMUM_PLAY_TIME;

            val remainingTime = requiredTime - playedTime;

            session.sendLocalizedMessage("forum_cannot_register",
                    "%time%", Duration.durationWordsSec(language, requiredTime),
                    "%remaining%", Duration.durationWordsSec(language, remainingTime));
            return;
        }

        val playerName = gamer.getName().toLowerCase();
        val confirmation = confirmationMap.get(playerName);

        if (confirmation != null) {
            if (session.getArgumentCount() == 0) {
                session.sendLocalizedMessage("forum_command_usage_code");
                return;
            }

            if (!session.getArgument(0).equals(confirmation.getConfirmationCode())) {
                val retries = confirmation.decrementRetries();

                if (retries < 0) {
                    session.sendLocalizedMessage("forum_tries_limit");
                    confirmationMap.remove(playerName);
                    return;
                }

                session.sendLocalizedMessage("forum_invalid_code", "%tries%", String.valueOf(retries));
                return;
            }

            val password = PasswordHashing.generateRandomPassword(12);

            confirmationMap.remove(playerName);

            session.sendLocalizedMessage("forum_account_creation_in_progress");

            xenApi.createAccount(gamer.getName(), confirmation.getEmail(),
                    password, gamer)
                    .exceptionally(throwable -> {
                        log.error("Error while creating account for {}", playerName, throwable);
                        return null;
                    })
                    .thenAccept(result -> {
                if (result == null || !result.success() || result.userId() == null) {
                    session.sendLocalizedMessage("forum_registration_failed");
                    return;
                }

                coreForumPlugin.getMailer().sendMessage(language.getMessage("forum_registration_success_subject"),
                        language.getMessage("forum_registration_success_body",
                                "%player%", gamer.getName(), "%login%", gamer.getName(), "%password%", password),
                        confirmation.getEmail(), ex -> {
                            gamer.sendLocalizedMessage("forum_account_created_but_email_failed",
                                    "%login%", gamer.getName(),
                                    "%password%", password);
                        });

                session.sendLocalizedMessage("forum_account_created",
                        "%login%", gamer.getName(),
                        "%password%", password);

                coreForumPlugin.getForumSqlManager().addIdToCache(gamer, result.userId());

                coreForumPlugin.getCore().getScheduler().schedule(
                        () -> coreForumPlugin.getForumSqlManager().linkGameName(gamer),
                        2, TimeUnit.SECONDS
                );
            });

            return;
        }

        val userId = coreForumPlugin.getForumSqlManager().getForumId(gamer);

        if (userId > 0) {
            session.sendLocalizedMessage("forum_command_register_account_exists");
            return;
        }

        if (session.getArgumentCount() == 0) {
            session.sendLocalizedMessage("forum_command_register_usage_email");
            return;
        }

        val existingCooldown = PlayerCooldownUtil.getCooldown(gamer.getPlayerId(), COOLDOWN_KEY);

        if (existingCooldown != null && !existingCooldown.isExpired()) {
            session.sendLocalizedMessage("forum_command_register_cooldown",
                    "%time%", existingCooldown.remainingFormatted(language));
            return;
        }

        val email = session.getArgument(0).trim();

        if (!EMAIL_REGEX.matcher(email).matches()) {
            session.sendLocalizedMessage("forum_command_register_invalid_email");
            return;
        }

        if (TempMailUtil.isTempMail(email)) {
            session.sendLocalizedMessage("forum_command_register_temp_email");
            return;
        }

        coreForumPlugin.getForumSqlManager().checkEmailAvailability(email).thenAccept(available -> {
            if (!available) {
                session.sendLocalizedMessage("forum_email_already_in_use");
                return;
            }

            val sentConfirmation = new Confirmation(email, PasswordHashing.generateRandomPassword(12));
            confirmationMap.put(playerName, sentConfirmation);

            val subject = language.getMessage("forum_linkage_email_subject");
            val body = language.getMessage("forum_linkage_email_body",
                    "%player%", gamer.getName(), "%code%", sentConfirmation.getConfirmationCode());

            coreForumPlugin.getMailer().sendMessage(subject, body, email, ex -> {
                PlayerCooldownUtil.removeCooldown(gamer.getPlayerId(), COOLDOWN_KEY);

                session.sendLocalizedMessage("forum_command_register_email_send_exception");
                confirmationMap.remove(playerName);
            });

            session.sendLocalizedMessage("forum_command_register_email_sent");
            PlayerCooldownUtil.addCooldown(gamer.getPlayerId(), COOLDOWN_KEY, 5, TimeUnit.MINUTES);
        });
    }

    @RequiredArgsConstructor
    @Getter
    public static class Confirmation {

        private final String email;
        private final String confirmationCode;

        private int retries = 3;

        public int decrementRetries() {
            return --retries;
        }
    }

}
