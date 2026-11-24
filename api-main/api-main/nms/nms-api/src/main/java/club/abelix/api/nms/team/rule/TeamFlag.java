package club.abelix.api.nms.team.rule;

import lombok.Getter;

public enum TeamFlag {

    FRIENDLY_FIRE, SEE_INVISIBLE;

    @Getter
    private final int mask = 1 << ordinal();

}
