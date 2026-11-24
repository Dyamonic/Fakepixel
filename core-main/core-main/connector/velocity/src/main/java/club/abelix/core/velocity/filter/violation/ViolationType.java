package club.abelix.core.velocity.filter.violation;

import lombok.RequiredArgsConstructor;

/**
 * Created by saharNooby on 14:46 05.12.2017
 */
@RequiredArgsConstructor
public enum ViolationType {

    FLOOD(1.0),
    ADVERTISING(3.0),
    SWEARING(1.8),
    FAST_PACKETS(1.3),
    FAST_CHAT(0.5);

    public final double levelDelta;
}
