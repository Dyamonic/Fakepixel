package club.abelix.core.telegram.command.sender;

import club.abelix.commons.util.color.ChatColor;
import club.abelix.commons.util.regex.Patterns;
import club.abelix.core.app.api.command.CommandSender;
import club.abelix.core.telegram.ImportantTelegramChats;
import club.abelix.core.telegram.TelegramBotPlugin;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.ReplyParameters;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.jetbrains.annotations.NotNull;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TelegramCommandSender extends ProxiedCommandSender {

    public static @NotNull CommandSender create(
            final @NonNull CommandSender parent,
            final @NonNull Message originalMessage
    ) {
        return new TelegramCommandSender(parent, originalMessage);
    }

    Message originalMessage;

    private TelegramCommandSender(
            final CommandSender parent,
            final Message originalMessage
    ) {
        super(parent);
        this.originalMessage = originalMessage;
    }

    @Override
    public void sendMessage(final String message) {
        if (TelegramBotPlugin.getInstance().isPaused()) {
            return;
        }

        var formattedMessage = ChatColor.stripColor(message);
        val chatId = originalMessage.chat().id();

        if (ImportantTelegramChats.shouldHideIps(chatId)) {
            formattedMessage = ChatColor.stripColor(message).replaceAll(Patterns.DIGIT_IP.pattern(), "[protected]");
        }

        val request = new SendMessage(originalMessage.chat().id(), formattedMessage);
        request.replyParameters(new ReplyParameters(originalMessage.messageId()));

        val botPlugin = TelegramBotPlugin.getInstance();
        botPlugin.printMessage(request);
    }

    @Override
    public void sendRawMessage(final String message) {
        // not currently supported
    }

    @Override
    public boolean isOnline() {
        return false;
    }
}
