package club.abelix.commons.translate.api;

import org.jetbrains.annotations.NotNull;

/**
 * @author milansky
 */
public interface TranslateLanguage {
    @NotNull String identifier();

    static @NotNull TranslateLanguage of(final @NotNull String identifier) {
        return () -> identifier;
    }
}
