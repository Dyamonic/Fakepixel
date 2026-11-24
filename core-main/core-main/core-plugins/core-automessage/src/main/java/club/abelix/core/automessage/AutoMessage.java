package club.abelix.core.automessage;

import club.abelix.commons.language.Language;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
@Getter
public class AutoMessage {

    private final String localizationKey;

    private final Map<Language, String> messages;

    public String getMessage(@NonNull Language language) {
        return messages.get(language);
    }

}
