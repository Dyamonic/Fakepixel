package club.abelix.core.telegram.command.impl;

import com.pengrad.telegrambot.model.Message;
import lombok.NonNull;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import club.abelix.core.telegram.TelegramBotPlugin;
import club.abelix.core.telegram.command.BotCommand;
import club.abelix.core.telegram.user.BotUser;

public final class PauseCommand extends BotCommand {
    public PauseCommand() {
        super("pause", "пауза");

        setOnlyAdmins(true);
        setOnlyChats(true);
    }

    @Override
    protected void execute(BotUser botUser, @Nullable Message message, long chatId, @NotNull @NonNull String[] args,
                           @NonNull TelegramBotPlugin botPlugin) {
        val status = botPlugin.isPaused();
        botPlugin.setPaused(!status);

        botPlugin.printMessage(chatId, "✅ Статус паузы изменен на " + !status);
    }
}
