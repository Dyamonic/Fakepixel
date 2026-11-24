package club.abelix.games.fastbridge.commons.utils;

import lombok.experimental.UtilityClass;

import java.text.DecimalFormat;

@UtilityClass
public class RecordFormat {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.000");

    public String formatTime(double time) {
        if (time <= 0) return "-.---";

        return DECIMAL_FORMAT.format(time);
    }

    public String formatTime(int time) {
        return formatTime(time / 1000D);
    }

    public String formatTime(long time) {
        return formatTime(time / 1000D);
    }

}
