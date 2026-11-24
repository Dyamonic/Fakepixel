package club.abelix.commons.translate.api;

import org.jetbrains.annotations.NotNull;

/**
 * @author milansky
 */
public record TranslateRequest(
        @NotNull TranslateLanguage from,
        @NotNull TranslateLanguage to,
        @NotNull String text
) {}
