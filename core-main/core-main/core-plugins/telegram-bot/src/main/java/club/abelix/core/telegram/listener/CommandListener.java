package club.abelix.core.telegram.listener;

import club.abelix.core.app.Async;
import club.abelix.core.app.api.event.EventPriority;
import club.abelix.core.app.api.plugin.Listener;
import club.abelix.core.telegram.TelegramBotPlugin;
import club.abelix.core.telegram.event.ButtonClickEvent;
import club.abelix.core.telegram.event.MessageEvent;
import com.pengrad.telegrambot.model.Message;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.greenrobot.eventbus.Subscribe;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

@RequiredArgsConstructor
public final class CommandListener implements Listener {

    private final TelegramBotPlugin plugin;

    @Subscribe(priority = EventPriority.LOWEST)
    public void onMessage(final @NonNull MessageEvent event) {
        val message = event.getMessage();

        if (message.text() == null || message.from().isBot()) {
            return;
        }

        var messageText = message.text().trim();

        if (messageText.length() < 2) {
            return;
        }

        executeCommand(message, messageText, message.chat().id(), message.from().id());
    }

    @Subscribe
    public void onButton(final @NonNull ButtonClickEvent event) {
        val callbackQuery = event.getCallbackQuery();
        val data = callbackQuery.data();

        if (data == null) {
            return;
        }

        val dataText = data.trim();

        if (data.length() < 2) {
            return;
        }

        val from = callbackQuery.from();

        if (from.isBot()) {
            return;
        }

        executeCommand(null, dataText, from.id(), from.id());
    }

    private void executeCommand(@Nullable Message message, @NonNull String messageText, long chatId, long userId) {
        val usedFullCommand = messageText.startsWith("!") || messageText.startsWith("*") || messageText.startsWith("/start");

        if (usedFullCommand) {
            messageText = messageText.substring(1);
        }

        val commandArray = messageText.split(" ");
        val commandArgs = Arrays.copyOfRange(commandArray, 1, commandArray.length);

        val cmd = plugin.getCommand(commandArray[0].toLowerCase());

        if (cmd == null) {
            return;
        }

        if (!usedFullCommand && !cmd.isNoobCommand()) {
            return;
        }

        Async.run(() -> cmd.dispatchCommand(message, chatId, userId, commandArgs, plugin));
    }

}
