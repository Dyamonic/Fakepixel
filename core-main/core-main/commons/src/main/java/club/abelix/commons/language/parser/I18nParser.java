package club.abelix.commons.language.parser;

import club.abelix.commons.language.Language;
import club.abelix.commons.language.value.LanguageValue;

public interface I18nParser {

    I18nParser getParent();

    void registerValue(String name, LanguageValue value);

    LanguageValue getValue(String name);

    void clearCache();

    String parse(Language language, String text);

}
