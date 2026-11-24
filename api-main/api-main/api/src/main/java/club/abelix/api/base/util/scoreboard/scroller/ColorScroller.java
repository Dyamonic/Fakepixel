package club.abelix.api.base.util.scoreboard.scroller;

import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Старый код с максмайна, который лень менять
 */
public class ColorScroller implements Scroller {

    // --------------------------------------------------------------------------

    private static final Pattern SPECIAL_PATTERN_MATCH = Pattern.compile("(@)");

    // --------------------------------------------------------------------------

    private final String[] masks;
    private final String mainColor;
    private final String text;

    private int currentPosition = -1;

    public ColorScroller(@NonNull String text, @NonNull String mainColor, @NonNull String... masks) {
        List<String> list = new ArrayList<>();
        Collections.addAll(list, masks);
        list.add(mainColor);

        this.masks = list.toArray(new String[0]);
        this.mainColor = mainColor;
        this.text = text;
    }

    private static String getPattern(@NonNull String string, int... positions) {
        StringBuilder sb = new StringBuilder(string);

        for (int position : positions) {
            sb.insert(position + 1, "@");
        }

        return sb.toString();
    }

    private static String colorizeString(@NonNull String pattern, @NonNull String... colors) {
        if (colors.length == 0) return SPECIAL_PATTERN_MATCH.matcher(pattern).replaceAll("");

        return applyPattern(pattern, new Function<>() {
            int index = -1;

            @Override
            public String apply(String s) {
                if (index++ >= colors.length) {
                    return null;
                }

                return colors[index];
            }
        });
    }

    private static String applyPattern(String pattern, Function<String, String> function) {
        Matcher matcher = SPECIAL_PATTERN_MATCH.matcher(pattern);

        StringBuffer output = new StringBuffer();

        while (matcher.find()) {
            String s = function.apply(matcher.group());

            if (s == null || s.isEmpty()) {
                break;
            }

            matcher.appendReplacement(output, s);
        }

        matcher.appendTail(output);

        return output.toString();
    }

    @Override
    public String getNext(boolean recursively) {
        if (recursively && !hasNext()) currentPosition = -1;
        if (!hasNext()) throw new IllegalStateException("Scrolling has end.");

        int[] positions = new int[masks.length];
        for (int i = 0; i < masks.length; i++) {
            positions[i] = currentPosition + 2 * i;
        }

        String pattern = getPattern(text, positions);
        currentPosition++;

        return (currentPosition == 0 ? masks[0] : mainColor) + colorizeString(pattern, masks);
    }

    @Override
    public boolean hasNext() {
        return this.currentPosition + (2 * (masks.length - 1)) <= text.length();
    }

}
