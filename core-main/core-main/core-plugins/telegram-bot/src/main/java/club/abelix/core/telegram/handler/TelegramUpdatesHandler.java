package club.abelix.core.telegram.handler;

import club.abelix.commons.util.time.CooldownUtil;
import club.abelix.commons.util.time.TimeUtil;
import club.abelix.core.telegram.ImportantTelegramChats;
import club.abelix.core.telegram.TelegramBotPlugin;
import club.abelix.core.telegram.event.ButtonClickEvent;
import club.abelix.core.telegram.event.MessageEvent;
import club.abelix.network.common.logging.Logging;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.LeaveChat;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public final class TelegramUpdatesHandler implements UpdatesListener {

    private final TelegramBotPlugin plugin;

    @Override
    public int process(final @NonNull List<Update> updates) {
        for (val update : updates) {
            handleUpdate(update);
        }

        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    private void handleUpdate(@NonNull Update update) {
        val chatMemberUpdated = update.myChatMember();

        if (chatMemberUpdated != null) {
            val chat = chatMemberUpdated.chat();
            val chatType = chat.type();

            if (chatType == Chat.Type.Private) {
                return;
            }

            val chatId = chat.id();

            if (ImportantTelegramChats.isImportantChat(chatId)) {
                return;
            }

            val newStatus = chatMemberUpdated.newChatMember().status();

            if (newStatus != null && "member".equalsIgnoreCase(newStatus.name())) {
                Logging.IMP.info("[TG-Bot] Bot has been added to an invalid chat ({}), leaving it...", chatId);
                plugin.printMessage(chatId, "м-м-мама мне не разрешает общаться с незнакомцами o_0");
                plugin.getBot().execute(new LeaveChat(chatId));
                return;
            }
        }

        val message = update.message();

        if (message != null) {
            if (checkForNonImportantChat(message.chat())) {
                return;
            }

            if (TimeUtil.getNow() > message.date() + 10) {
                return;
            }

            val chatId = message.chat().id();
            val userId = message.from().id();

            if (checkCooldown(userId)) {
                return;
            }

            plugin.getCore().getPluginManager().callEvent(new MessageEvent(chatId, userId, message));
            return;
        }

        val callbackQuery = update.callbackQuery();

        if (callbackQuery != null) {
            val userId = update.callbackQuery().from().id();

            val sendResponse = new AnswerCallbackQuery(callbackQuery.id());
            plugin.getBot().execute(sendResponse);

            if (checkCooldown(userId)) {
                return;
            }

            plugin.getCore().getPluginManager().callEvent(new ButtonClickEvent(userId, userId, update.callbackQuery()));
        }
    }

    private boolean checkCooldown(final long userId) {
        val cooldownKey = "bot_command" + userId;

        if (CooldownUtil.hasCooldown(cooldownKey)) {
            return true;
        }

        CooldownUtil.addCooldown(cooldownKey, 1, TimeUnit.SECONDS);
        return false;
    }

    private boolean checkForNonImportantChat(@NonNull Chat chat) {
        val chatType = chat.type();

        if (chatType == Chat.Type.Private) {
            return false;
        }

        val chatId = chat.id();

        if (!ImportantTelegramChats.isImportantChat(chatId)) {
            Logging.IMP.info("[TG-Bot] Received an update from an invalid chat ({}), leaving it...", chatId);
            plugin.getBot().execute(new LeaveChat(chatId));
            return true;
        }

        return false;
    }
}
