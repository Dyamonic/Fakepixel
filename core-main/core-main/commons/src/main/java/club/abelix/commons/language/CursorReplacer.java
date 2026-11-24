package club.abelix.commons.language;

import club.abelix.commons.util.string.StringUtil;
import lombok.NonNull;

/**
 * Какой-то обработчик значения, которое будет подставлено вместо курсора
 * Например, если у нас есть локализации:
 * invite_name: 'Приглашение'
 * invite_sent: 'Вы отправили игроку %player% {invite_name:lower}'
 * <p>
 * Таким образом получится не 'Вы отправили игроку %player% Приглашение', а
 * красивая запись 'Вы отправили игроку %player% приглашение' (буква П маленькая)
 */
public enum CursorReplacer {

    LOWER {
        @Override
        protected String replace(@NonNull String value) {
            return value.toLowerCase();
        }
    },
    UPPER {
        @Override
        protected String replace(@NonNull String value) {
            return value.toUpperCase();
        }
    },
    FIRST_UP {
        @Override
        protected String replace(@NonNull String value) {
            return value.length() > 1 ? StringUtil.capitalize(value) : UPPER.replace(value);
        }
    },
    WORD_UP {
        @Override
        protected String replace(@NonNull String value) {
            String[] words = value.split(" ");

            for (String word : words) {
                value = value.replaceFirst(word, FIRST_UP.replace(word));
            }

            return value;
        }
    };

    public static CursorReplacer get(@NonNull String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    // ---------------------------------------------------------------

    protected abstract String replace(@NonNull String value);

}
