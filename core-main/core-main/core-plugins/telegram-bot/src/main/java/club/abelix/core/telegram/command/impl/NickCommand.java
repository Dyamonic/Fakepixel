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

public final class NickCommand extends BotCommand {
    public NickCommand() {
        super("nick", "ник", "whois", "кто");

        setOnlyChats(true);
//        setNoobCommand(true);
    }

    @Override
    protected void execute(BotUser botUser, @Nullable Message message, long chatId, @NotNull @NonNull String[] args,
                           @NonNull TelegramBotPlugin botPlugin) {
        if (message == null) {
            return;
        }

        val reply = message.replyToMessage();
        if (reply == null) {
            botPlugin.printMessage(chatId, "\uD83D\uDEAB Перешлите сообщение пользователя, ник которого хотите узнать");
            return;
        }

        val userId = reply.from().id();
        val foundedUser = BotUserSqlHandler.INSTANCE.getBotUser(userId);

        if (foundedUser == BotUser.EMPTY_BOT_USER) {
            botPlugin.printMessage(chatId, "\uD83D\uDEAB Пользователь не найден в базе данных");
            return;
        }

        if (foundedUser.getPrimaryAccountName() == null) {
            botPlugin.printMessage(chatId, "\uD83D\uDEAB У пользователя нет привязанных аккаунтов");
            return;
        }

        botPlugin.printMessage(chatId, "✅ Ник пользователя: " + foundedUser.getPrimaryAccountName());
    }
}
