package club.abelix.api.base.chat.translation;

import club.abelix.commons.language.Language;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Unidentified Person
 */
public class ChatTranslationManager {

    public static final ChatTranslationManager INSTANCE = new ChatTranslationManager();

    private final AtomicInteger counter = new AtomicInteger();
    private final Deque<TranslatableChatMessage> messages = new ArrayDeque<>(100);

    public TranslatableChatMessage findMessage(int id) {
        synchronized (messages) {
            Iterator<TranslatableChatMessage> iterator = messages.descendingIterator();

            while (iterator.hasNext()) {
                TranslatableChatMessage message = iterator.next();

                if (message.getId() == id) {
                    return message;
                }
            }
        }

        return null;
    }

    public TranslatableChatMessage handleMessage(String text, Language language) {
        TranslatableChatMessage message = new TranslatableChatMessage(counter.getAndIncrement(), text, language);

        synchronized (messages) {
            if (messages.size() > 100) {
                messages.removeFirst();
            }

            messages.add(message);
        }

        return message;
    }

}
