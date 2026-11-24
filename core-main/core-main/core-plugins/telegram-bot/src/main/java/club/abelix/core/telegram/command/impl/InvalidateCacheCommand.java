package club.abelix.core.telegram.command.impl;

import com.pengrad.telegrambot.model.Message;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import club.abelix.core.telegram.TelegramBotPlugin;
import club.abelix.core.telegram.command.BotCommand;
import club.abelix.core.telegram.user.BotUser;
import club.abelix.core.telegram.user.sql.BotUserSqlHandler;


public final class InvalidateCacheCommand extends BotCommand {
    public InvalidateCacheCommand() {
        super("invalidate");

        setOnlyAdmins(true);
    }

    @Override
    protected void execute(BotUser botUser, @Nullable Message message, long chatId, @NotNull @NonNull String[] args,
                           @NonNull TelegramBotPlugin botPlugin) {
        if (args.length < 1) {
            botPlugin.printMessage(chatId, "\uD83D\uDEAB Недостаточно аргументов");
            return;
        }

        long targetId;
        String targetNick;

        try {
            targetId = Long.parseLong(args[0]);
            targetNick = null;
        } catch (NumberFormatException e) {
            targetId = 0;
            targetNick = args[0];
        }

        BotUser targetUser;

        if (targetNick != null) {
            targetUser = BotUserSqlHandler.INSTANCE.getBotUser(targetNick.toLowerCase());
        } else {
            targetUser = BotUserSqlHandler.INSTANCE.getBotUser(targetId);
        }

        if (targetUser == BotUser.EMPTY_BOT_USER) {
            botPlugin.printMessage(chatId, "\uD83D\uDEAB Пользователь не найден");
            return;
        }

        BotUserSqlHandler.INSTANCE.invalidateUserFromCache(botUser.getTelegramId());
        botPlugin.printMessage(chatId, "✅ Пользователь удалён из кеша");
    }
}
