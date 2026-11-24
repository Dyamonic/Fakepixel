package club.abelix.api.base.chat;

import club.abelix.commons.language.Language;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

/**
 * @author Unidentified Person
 */
public class ChatWord {

    private final boolean onlyDigits;
    private final ChatMessage message;
    private final int begin;
    private final int end;

    private Boolean mention;
    private String text;

    public ChatWord(boolean onlyDigits, ChatMessage message, int begin, int end) {
        this.onlyDigits = onlyDigits;
        this.message = message;
        this.begin = begin;
        this.end = end;
    }

    public int length() {
        return end - begin;
    }

    public boolean onlyDigits() {
        return onlyDigits;
    }

    public Language getLanguage() {
        float maxPercentage = 0;
        Language found = null;

        for (Language language : Language.VALUES) {
            float percentage = getLanguagePercentage(language);

            if (percentage == 1F) {
                return language;
            }

            if (maxPercentage < percentage) {
                maxPercentage = percentage;
                found = language;
            }
        }

        return found;
    }

    public float getLanguagePercentage(Language language) {
        int alphabetCharacters = 0;

        for (int i = begin; i < end; i++) {
            char value = message.charAt(i);

            if (language.isInAlphabet(value)) {
                alphabetCharacters++;
            }
        }

        return (float) alphabetCharacters / length();
    }

    public boolean isMention() {
        if (mention == null) {
            mention = getText().equals("@everyone") || Bukkit.getPlayer(getText()) != null;
        }

        return mention;
    }

    public String getText() {
        if (text == null) {
            text = ChatColor.stripColor(message.sub(begin, end));
        }

        return text;
    }

    public ChatMessage getMessage() {
        return message;
    }
}
