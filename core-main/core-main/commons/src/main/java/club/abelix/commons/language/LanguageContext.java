package club.abelix.commons.language;

import club.abelix.commons.util.array.ObjectLinkedBuf;
import lombok.Getter;
import lombok.NonNull;

import java.util.function.Function;

/**
 * Нужно для отправки каких-то глобальных сообщений
 *
 * @author artem, Kambet
 */
public class LanguageContext {
    @Getter
    private final String languagePrimaryKey;
    private final Function<Language, String> messageProvider;

    public LanguageContext(@NonNull String key, @NonNull Function<Language, ObjectLinkedBuf> paramsFactory) {
        this.languagePrimaryKey = key;
        this.messageProvider = language -> language.getMessage(key, paramsFactory.apply(language).toArray());
    }

    public LanguageContext(@NonNull Function<Language, String> messageProvider) {
        this.languagePrimaryKey = null;
        this.messageProvider = messageProvider;
    }

    public String format(Language language) {
        return messageProvider.apply(language);
    }

}
