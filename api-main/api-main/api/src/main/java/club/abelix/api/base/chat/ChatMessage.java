package club.abelix.api.base.chat;

import club.abelix.commons.language.Language;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Unidentified Person
 */
public class ChatMessage {

    private final StringBuilder message;
    private List<ChatWord> words;

    public ChatMessage(String message) {
        this.message = new StringBuilder(message);
    }

    public static ChatMessage parse(String message) {
        ChatMessage chatMessage = new ChatMessage(message);
        chatMessage.words = new LinkedList<>();

        boolean onlyDigits = true;
        int wordBegin = 0;

        for (int i = 0; i <= message.length(); i++) {
            int value = i == message.length() ? -1 : message.charAt(i);

            if (value == ' ' || value == -1) {
                ChatWord word = new ChatWord(onlyDigits, chatMessage, wordBegin, i);

                wordBegin = i + 1;
                onlyDigits = true;

                chatMessage.words.add(word);
            }

            if (onlyDigits && Character.isAlphabetic(value)) {
                onlyDigits = false;
            }
        }

        return chatMessage;
    }

    public List<ChatWord> getWords() {
        return words;
    }

    public int getLength() {
        int len = 0;

        for (ChatWord word : words) {
            if (!word.onlyDigits() && !word.isMention()) {
                len += word.length();
            }
        }

        return len;
    }

    public Language getLanguage() {
        float[] results = new float[Language.VALUES.length];

        float maxPercentage = 0;
        Language result = null;

        for (ChatWord word : words) {
            if (!word.onlyDigits() && !word.isMention()) {
                for (Language language : Language.VALUES) {
                    results[language.ordinal()] += word.getLanguagePercentage(language);

                    float totalPercentage = results[language.ordinal()];

                    if (totalPercentage > maxPercentage) {
                        maxPercentage = totalPercentage;
                        result = language;
                    }
                }
            }
        }

        return result;
    }

    public char charAt(int idx) {
        return message.charAt(idx);
    }

    public String sub(int begin, int end) {
        return message.substring(begin, end);
    }
}
