package club.abelix.core.velocity.filter.violation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Violation {

    private final ViolationType type;
    private final long timestamp = System.currentTimeMillis();
}
