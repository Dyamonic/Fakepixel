package club.abelix.games.buildffa.game.level;

import lombok.Getter;
import lombok.NonNull;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class Leveling {
    private static final DecimalFormat GROUPED_FORMAT;
    @Getter
    private static final int EXP_PER_LEVEL = 5000; // Опыт до следующего уровня

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(',');

        GROUPED_FORMAT = new DecimalFormat();
        GROUPED_FORMAT.setDecimalFormatSymbols(symbols);
        GROUPED_FORMAT.setGroupingSize(3);
    }

    public static int getLevel(int exp) {
        return exp < 0 ? 1 : (exp / EXP_PER_LEVEL) + 1;
    }

    public static double getExactLevel(int exp) {
        return getLevel(exp) + getPercentageToNextLevel(exp);
    }

    public static int getExpFromLevelToNext(int level) {
        return (level - 1) * EXP_PER_LEVEL;
    }

    public static int getTotalExpToLevel(double level) {
        return (int) (EXP_PER_LEVEL * (level - 1));
    }

    public static float getPercentageToNextLevel(int exp) {
        double level = getLevel(exp);
        double currentLevelExp = getTotalExpToLevel(level);
        double nextLevelExp = currentLevelExp + EXP_PER_LEVEL;

        if (exp >= nextLevelExp) {
            return 1.0f;
        } else {
            return (float) (exp - currentLevelExp) / EXP_PER_LEVEL;
        }
    }

    public static String formatK(double xp, @NonNull String color) {
        if (xp < 1000) {
            return String.format("%.0f%s", xp, color);
        } else {
            double formattedNumber = xp / 1000.0;
            String formatString = (xp < 10000) ? "%.1f%s" : "%.0f%s";
            return String.format(formatString, formattedNumber, "k" + color);
        }
    }

    public static String formatGrouped(int exp) {
        return GROUPED_FORMAT.format(exp);
    }

    public static String getScale(int size, double percent, String fill, String done, String undone) {
        int part = (int) (size * percent);

        StringBuilder scale = new StringBuilder(size + 4);
        if (part > 0) {
            scale.append(done);
        }

        for (int i = 0; i < size; i++) {
            if (i == part) {
                scale.append(undone);
            }

            scale.append(fill);
        }

        return scale.toString();
    }
}

