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

public class AccountPrimaryCommand extends BotCommand {

    public AccountPrimaryCommand() {
        super("основа", "primary");
    }

    @Override
    protected void execute(BotUser botUser, @Nullable Message message, long chatId, @NotNull @NonNull String[] args,
                           @NonNull TelegramBotPlugin botPlugin) {
        val linkedAccounts = botUser.getLinkedAccounts();

        if (linkedAccounts.size() == 1) {
            args = new String[]{linkedAccounts.get(0)};
        }

        val language = botUser.getLanguage();
        if (args.length == 0) {
            botPlugin.printMessage(chatId, language.getMessage("core_telegram_command_primary_usage",
                    "%emoji_1%", "⚠", "%emoji_2%", "⭐"));
            return;
        }

        val playerName = args[0];

        if (!botUser.hasLinkedAccount(playerName)) {
            botPlugin.printMessage(chatId, language.getMessage("core_telegram_command_no_account",
                    "%emoji_1%", "\uD83D\uDEAB", "%name%", playerName));
            return;
        }

        if (botUser.hasPrimaryAccount() && botUser.getPrimaryAccountName().equalsIgnoreCase(playerName)) {
            botPlugin.printMessage(chatId, language.getMessage("core_telegram_command_primary_already",
                    "%emoji_1%", "\uD83D\uDEAB", "%name%", playerName));
            return;
        }

        BotUserSqlHandler.INSTANCE.linkPrimaryAccount(botUser, playerName);

        if (linkedAccounts.size() > 1) {
            botPlugin.printMessage(chatId, language.getMessage("core_telegram_command_primary_set",
                    "%emoji_1%", "✅", "%name%", playerName));
        } else {
            botPlugin.printMessage(chatId, language.getMessage("core_telegram_command_primary_set_auto",
                    "%emoji_1%", "✅", "%name%", playerName));
        }
    }
}
