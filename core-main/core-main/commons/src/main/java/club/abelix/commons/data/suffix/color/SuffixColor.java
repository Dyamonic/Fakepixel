package club.abelix.commons.data.suffix.color;

import club.abelix.commons.language.Language;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum SuffixColor {

    AQUA(1, 3, 'b'),
    BLUE(2, 11, '9'),
    DARK_AQUA(3, 9, '3'),
    DARK_GREEN(4, 13, '2'),
    DARK_PURPLE(5, 10, '5'),
    GREEN(6, 5, 'a'),
    GOLD(7, 1, '6'),
    GRAY(8, 8, '7'),
    PINK(9, 6, 'd'),
    RED(10, 14, 'c'),
    YELLOW(11, 4, 'e'),
    WHITE(12, 0, 'f'),
    BLACK(13, 15, '0'),
    DARK_BLUE(14, 11, '1'),
    DARK_RED(15, 14, '4');

    private static final TIntObjectMap<SuffixColor> BY_ID = new TIntObjectHashMap<>();

    static {
        for (SuffixColor color : SuffixColor.values()) {
            BY_ID.put(color.getId(), color);
        }
    }

    private final int id;
    private final int durability;
    private final char colorCode;

    // ------------------------------------------------------------------------------

    public static SuffixColor getById(int id) {
        return BY_ID.get(id);
    }

    public String getDisplayName(@NonNull Language language) {
        return "§" + colorCode + getColorName(language);
    }

    public String getColorName(@NonNull Language language) {
        return language.getMessage("color_" + name());
    }

}
