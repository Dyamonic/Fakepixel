package club.abelix.core.telegram.command.impl;

import club.abelix.commons.language.Language;
import com.pengrad.telegrambot.model.Message;
import lombok.NonNull;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import club.abelix.core.telegram.TelegramBotPlugin;
import club.abelix.core.telegram.command.BotCommand;
import club.abelix.core.telegram.user.BotUser;
import club.abelix.core.telegram.user.sql.BotUserSqlHandler;

public class AccountLanguageCommand extends BotCommand {

    public AccountLanguageCommand() {
        super("setuserlanguage");

        setNoobCommand(false);
        setOnlyPrivateMessages(true);
    }

    @Override
    protected void execute(BotUser botUser, @Nullable Message message, long chatId, @NotNull @NonNull String[] args,
                           @NonNull TelegramBotPlugin botPlugin) {
        if (args.length == 0) {
            return;
        }

        if (botUser.getLanguage() != null && args.length > 1 && args[1].equals("auto")) {
            return;
        }

        val languageName = args[0];
        val language = Language.getByName(languageName);

        if (!language.getShortName().equalsIgnoreCase(languageName)) {
            botPlugin.printMessage(chatId, language.getMessage("core_telegram_command_language_not_found",
                    "%emoji_1%", "❌"));
            return;
        }

        BotUserSqlHandler.INSTANCE.setLanguage(botUser, language);

        botPlugin.printMessage(chatId, language.getMessage("core_telegram_command_language_set",
                "%emoji_1%", "✅"));

        val helpCommand = botPlugin.getCommand("help");

        if (helpCommand != null) {
            helpCommand.dispatchCommand(message, chatId, botUser.getTelegramId(), new String[0], botPlugin);
        }
    }
}
