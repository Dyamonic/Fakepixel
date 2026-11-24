package club.abelix.core.telegram;

import com.pengrad.telegrambot.model.Message;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ImportantTelegramChats {

    /**
     * Консоль
     */
    STAFF_CHAT(-1003739978954L, 1221),

    /**
     * Стафф логи
     */
    STAFF_LOGS(-1003739978954L, 20);

    long chatId;
    int threadId;

    public static boolean isImportantChat(final long chatId) {
        for (val value : values()) {
            if (value.getChatId() == chatId) {
                return true;
            }
        }

        return false;
    }

    public static boolean canExecuteCommands(final Message message) {
        return STAFF_CHAT.getChatId() == message.chat().id() &&
                STAFF_CHAT.getThreadId() == message.messageThreadId();
    }

    public static boolean shouldHideIps(final long chatId) {
        return STAFF_CHAT.getChatId() == chatId;
    }
}
