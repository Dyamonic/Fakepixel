package club.abelix.api.base.chat.translation;

import club.abelix.commons.language.Language;
import org.bukkit.ChatColor;

import java.util.concurrent.CompletableFuture;

/**
 * @author Unidentified Person
 */
public class TranslatableChatMessage {

    private final int id;
    private final String text;
    private final Language language;

    private final CompletableFuture<String> translated = new CompletableFuture<>();

    public TranslatableChatMessage(int id, String text, Language language) {
        this.id = id;
        this.text = text;
        this.language = language;
    }

    public int getId() {
        return id;
    }

    public String getText() {
        return ChatColor.stripColor(text);
    }

    public Language getLanguage() {
        return language;
    }

    public CompletableFuture<String> getTranslated() {
        return translated;
    }

}
