package club.abelix.core.plugin.api;

import club.abelix.commons.language.Language;
import lombok.experimental.UtilityClass;
import lombok.val;

import java.util.Arrays;

@UtilityClass
public class LocalizationUtils {

    public String localize(String message, Language language) {
        val parts = message.split(", ");

        return language.getMessage(parts[0], (Object[]) Arrays.copyOfRange(parts, 1, parts.length));
    }

}
