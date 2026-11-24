package club.abelix.commons.data.trust;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TrustLevel {

    UNTRUSTED(0, true), // словарь + гпт

    LOW(1, true), // словарь + гпт

    MEDIUM(2, false), // словарь

    HIGH(3, false), // словарь

    VERIFIED(4, false);

    private final int level;

    private final boolean requiresModeration;
}
