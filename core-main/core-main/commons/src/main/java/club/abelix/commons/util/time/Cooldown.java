package club.abelix.commons.util.time;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Cooldown {

    private final int initialTime;
    private final int availableAt;

    public boolean isActive() {
        return availableAt > TimeUtil.getNow();
    }

}
