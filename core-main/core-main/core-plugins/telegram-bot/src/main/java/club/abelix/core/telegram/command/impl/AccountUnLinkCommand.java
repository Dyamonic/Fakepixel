package club.abelix.core.telegram.command.impl;

import com.pengrad.telegrambot.model.Message;
import lombok.NonNull;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import club.abelix.core.telegram.TelegramBotPlugin;
import club.abelix.core.telegram.command.BotCommand;
import club.abelix.core.telegram.user.BotUser;
import club.abelix.core.telegram.user.sql.BotUserSqlHandler;

public class AccountUnLinkCommand extends BotCommand {

    public AccountUnLinkCommand() {
        super("отвязать", "unlink");

        setNoobCommand(true);
        setOnlyPrivateMessages(true);
    }

    private static final String UNLINK_LOG_MESSAGE = "[TG-Bot] Id: %d отвязал аккаунт %s, все его аккаунты: %s";

    @Override
    protected void execute(BotUser botUser, @Nullable Message message, long chatId, @NotNull @NonNull String[] args,
                           @NonNull TelegramBotPlugin botPlugin) {
        val language = botUser.getLanguage();

        if (args.length == 0) {
            botPlugin.printMessage(chatId, language.getMessage("core_telegram_command_unlink_usage",
                    "%emoji_1%", "⚠", "%emoji_2%", "⭐"));
            return;
        }

        val playerName = args[0].replace("<", "").replace(">", "");

        if (!botUser.hasLinkedAccount(playerName)) {
            botPlugin.printMessage(chatId, language.getMessage("core_telegram_command_no_account",
                    "%emoji_1%", "\uD83D\uDEAB", "%name%", playerName));
            return;
        }

        BotUserSqlHandler.INSTANCE.unLinkBotAccount(botUser, playerName);

        botPlugin.printMessage(chatId, language.getMessage("core_telegram_command_unlink_success",
                "%emoji_1%", "❗", "%emoji_2%", "\uD83D\uDD25", "%name%", playerName));
    }
}
