package club.abelix.commons.util.language;

import club.abelix.commons.language.Language;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class LanguageUtil {

    public Language detectLanguage(@NonNull String text) {
        int cyr = 0, lat = 0;

        for (char ch : text.toCharArray()) {
            if (Character.UnicodeBlock.of(ch) == Character.UnicodeBlock.CYRILLIC) {
                cyr++;
            } else if (Character.UnicodeBlock.of(ch) == Character.UnicodeBlock.BASIC_LATIN) {
                if (Character.isLetter(ch)) lat++;
            }
        }

        int total = cyr + lat;
        if (total == 0) return null;

        double cyrRatio = (double) cyr / total;

        if (cyrRatio > 0.6) return Language.RUSSIAN;
        if (cyrRatio < 0.4) return Language.ENGLISH;

        return null;
    }
}
