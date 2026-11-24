package club.abelix.commons.data.joinmessage;

import club.abelix.commons.language.Language;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class JoinMessage {

    /**
     * Сообщение о входе по умолчанию. Будет использоваться, если
     * у игрока не выбрано сообщение о входе, или выбранное не приобретено
     */
    public static final JoinMessage DEFAULT
            = new JoinMessage(0, "default_join_message", "default_join_message_text", 0, false, false);

    int id;
    String name;
    String text;
    int cost;
    boolean availableForElite;
    boolean custom;

    public String getName(@NonNull Language language) {
        return language.getMessage(name);
    }

    public String getText(@NonNull Language language) {
        return language.isMessageTranslated(text) ?
                language.getMessage(text) :
                text;
    }

}
