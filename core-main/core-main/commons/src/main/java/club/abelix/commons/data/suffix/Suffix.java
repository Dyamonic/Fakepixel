package club.abelix.commons.data.suffix;

import club.abelix.commons.data.cases.rarity.Rarity;
import club.abelix.commons.data.suffix.color.SuffixColor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@Getter
@AllArgsConstructor
public class Suffix {

    private final int id;

    private final String text;

    private final SuffixColor color;

    private final int cost;

    private final Rarity rarity;

    /**
     * Получить отображаемый текст суффикса
     * вместе с его цветом
     *
     * @param color - предлагаемый цвет
     * @return - текст суффикса
     */
    public String getDisplaySuffix(@NonNull SuffixColor color) {
        return "§" + color.getColorCode() + text;
    }

    /**
     * Получить отображаемый текст суффикса
     * вместе с его цветом
     *
     * @return - текст суффикса
     */
    public String getDisplaySuffix() {
        return getDisplaySuffix(color);
    }

}
