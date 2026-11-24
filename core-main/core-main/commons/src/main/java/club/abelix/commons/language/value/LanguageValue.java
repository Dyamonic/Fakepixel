package club.abelix.commons.language.value;

import club.abelix.commons.language.Language;
import org.jetbrains.annotations.NotNull;

/**
 * @author Unidentified Person
 */
public interface LanguageValue {

    String process(@NotNull Language language, String[] args);

}
