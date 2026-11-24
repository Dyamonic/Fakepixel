package club.abelix.core.telegram.command.impl;

import club.abelix.commons.data.session.SessionSqlHandler;
import club.abelix.commons.language.LanguageContext;
import club.abelix.commons.util.password.PasswordHashing;
import club.abelix.core.app.CoreApplication;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.event.gamer.StaffMessageEvent;
import club.abelix.core.telegram.TelegramBotPlugin;
import club.abelix.core.telegram.command.BotCommand;
import club.abelix.core.telegram.user.BotUser;
import club.abelix.core.telegram.user.sql.AccountSqlHandler;
import com.pengrad.telegrambot.model.Message;
import lombok.NonNull;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AccountRecoveryCommand extends BotCommand {

    public AccountRecoveryCommand() {
        super("восстановить", "recovery");

        setNoobCommand(true);
        setOnlyPrivateMessages(true);
    }

    private static final String RECOVERY_LOG_MESSAGE = "[TG-Bot] Id: %d восстановил аккаунт %s, все его аккаунты: %s";

    @Override
    protected void execute(BotUser botUser, @Nullable Message message, long chatId, @NotNull @NonNull String[] args,
                           @NonNull TelegramBotPlugin botPlugin) {
        List<String> linkedAccounts = botUser.getLinkedAccounts();

        if (linkedAccounts.size() == 1) {
            args = new String[]{linkedAccounts.get(0)};
        }

        val language = botUser.getLanguage();
        if (args.length == 0) {
            botPlugin.printMessage(chatId, language.getMessage("core_telegram_command_recovery_usage",
                    "%emoji_1%", "⚠", "%emoji_2%", "⭐"));
            return;
        }

        val playerName = args[0].replace("<", "").replace(">", "");

        if (!botUser.hasLinkedAccount(playerName)) {
            botPlugin.printMessage(chatId, language.getMessage("core_telegram_command_no_account",
                    "%emoji_1%", "\uD83D\uDEAB", "%name%", playerName));
            return;
        }

        val player = Gamer.getOfflineGamer(playerName, gamer -> {
            gamer.loadPlayerId();
            gamer.loadPlayerName();
            gamer.loadAuthData();
            gamer.loadLastSession();
        });

        if (player != null) {
            if (player.getAuthData().isPremiumAuth()) {
                botPlugin.printMessage(chatId, language.getMessage("core_telegram_condition_premium_auth",
                        "%emoji_1%", "\uD83D\uDEAB"));
                return;
            }

            if (player.isOnline()) {
                player.kick(language.getMessage("core_telegram_command_recovery_kick_reason"));
            }

            val sessionId = player.getLastSession().getSessionId();

            SessionSqlHandler.INSTANCE.changeSessionEndTime(sessionId, null);
        }

        val password = PasswordHashing.generateRandomPassword(8);

        AccountSqlHandler.INSTANCE.changePlayerPassword(playerName, PasswordHashing.getHash(password));

        botPlugin.printMessage(chatId, language.getMessage("core_telegram_command_recovery_success",
                "%emoji_1%", "\uD83D\uDD10", "%emoji_2%", "⚠",
                "%name%", playerName, "%password%", password));

        val accounts = String.join(", ", botUser.getLinkedAccounts());
        val context = new LanguageContext(l -> String.format(RECOVERY_LOG_MESSAGE,
                botUser.getTelegramId(), playerName, accounts));

        val messageEvent = new StaffMessageEvent(context);
        CoreApplication.getInstance().getPluginManager().callEvent(messageEvent);
    }
}
