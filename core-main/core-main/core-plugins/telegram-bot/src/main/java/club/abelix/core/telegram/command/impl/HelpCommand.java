package club.abelix.core.telegram.command.impl;

import com.pengrad.telegrambot.model.Message;
import lombok.NonNull;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import club.abelix.core.telegram.TelegramBotPlugin;
import club.abelix.core.telegram.command.BotCommand;
import club.abelix.core.telegram.user.BotUser;

public class HelpCommand extends BotCommand {

    public HelpCommand() {
        super("помощь", "команды", "help", "commands");

        setNoobCommand(true);
        setOnlyPrivateMessages(true);
    }

    @Override
    protected void execute(BotUser botUser, @Nullable Message message, long chatId, @NotNull @NonNull String[] args,
                           @NonNull TelegramBotPlugin botPlugin) {
        val language = botUser.getLanguage();

        botPlugin.printMessage(chatId, language.getMessage("core_telegram_command_help",
                "%emoji_1%", "\uD83C\uDFA9", "%emoji_2%", "\uD83D\uDC64"));
    }
}
