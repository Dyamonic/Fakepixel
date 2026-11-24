package club.abelix.api.nms.entity.type.player;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SkinPart {

    // CAPE         - 00_00_00_01 - 1
    // JACKET       - 00_00_00_10 - 2
    // LEFT_SLEEVE  - 00_00_01_00 - 4
    // RIGHT_SLEEVE - 00_00_10_00 - 8
    // LEFT_PANTS   - 00_01_00_00 - 16
    // RIGHT_PANTS  - 00_10_00_00 - 32
    // HAT          - 01_00_00_00 - 64
    // ================================
    // TOTAL        - 01_11_11_11 - 127
    CAPE, JACKET, LEFT_SLEEVE, RIGHT_SLEEVE, LEFT_PANTS, RIGHT_PANTS, HAT;

    private final int mask = 1 << ordinal();

    public static final byte TOTAL = 0b01_11_11_11;

}
