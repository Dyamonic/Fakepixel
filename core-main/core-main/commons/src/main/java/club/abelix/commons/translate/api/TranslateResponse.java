package club.abelix.commons.translate.api;

import org.jetbrains.annotations.NotNull;

/**
 * @author milansky
 */
public record TranslateResponse(
        @NotNull String result,
        @NotNull TranslateRequest downstreamRequest
) {}
