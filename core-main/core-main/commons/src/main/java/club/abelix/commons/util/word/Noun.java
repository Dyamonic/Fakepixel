package club.abelix.commons.util.word;

import club.abelix.commons.language.Language;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

@EqualsAndHashCode
public class Noun {

    @Getter
    private final WordGender gender;

    private final Map<Language, String[]> forms = new EnumMap<>(Language.class);
    private final Map<Language, String[]> formsPlural = new EnumMap<>(Language.class);

    public Noun(@NonNull WordGender gender, @NonNull String... forms) {
        if (forms.length != 12) {
            throw new IllegalArgumentException(Arrays.toString(forms));
        }

        this.gender = gender;

        for (Language language : Language.VALUES) {
            String[] formsArray = this.forms.computeIfAbsent(language, k -> new String[6]);
            String[] formsPluralArray = this.formsPlural.computeIfAbsent(language, k -> new String[6]);

            for (int i = 0; i < 6 * 2; i += 2) {
                int row = i / 2;

                formsArray[row] = language.getMessage(forms[i]);
                formsPluralArray[row] = language.getMessage(forms[i + 1]);
            }
        }
    }

    public WordBuilder get(@NonNull Language language) {
        return new WordBuilder(language, this);
    }

    public WordBuilder get() {
        return new WordBuilder(Language.defaultLanguage(), this);
    }

    public WordBuilder with(@NonNull Language language, @NonNull Adjective adjective) {
        return get(language).adj(adjective);
    }

    public String selectForm(@NonNull Language language, @NonNull WordCase wordCase, boolean isPlural) {
        String[] currentForms = isPlural ? formsPlural.get(language) : forms.get(language);

        return currentForms[wordCase.ordinal()];
    }

    public String toString(@NonNull Language language) {
        return forms.get(language)[0];
    }

    @Override
    public String toString() {
        return toString(Language.defaultLanguage());
    }
}
