package club.abelix.commons.slf4j;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;

import java.util.HashMap;
import java.util.Map;

public class MinecraftFormattingConverter extends CompositeConverter<ILoggingEvent> {

    private enum Format {
        BLACK('0', 30),
        DARK_BLUE('1', 34),
        DARK_GREEN('2', 32),
        DARK_AQUA('3', 36),
        DARK_RED('4', 31),
        DARK_PURPLE('5', 35),
        GOLD('6', 33),
        GRAY('7', 37),
        DARK_GRAY('8', 90),
        BLUE('9', 94),
        GREEN('a', 92),
        AQUA('b', 96),
        RED('c', 91),
        LIGHT_PURPLE('d', 95),
        YELLOW('e', 93),
        WHITE('f', 97),

        BOLD('l', 1),
        ITALIC('o', 3),
        UNDERLINE('n', 4),
        STRIKETHROUGH('m', 9),

        RESET('r', 0);

        final char code;
        final int ansi;

        Format(char code, int ansi) {
            this.code = code;
            this.ansi = ansi;
        }
    }

    private static final Map<Character, Integer> CODE_TO_ANSI = new HashMap<>();

    static {
        for (Format f : Format.values()) {
            CODE_TO_ANSI.put(f.code, f.ansi);
        }
    }

    @Override
    protected String transform(ILoggingEvent event, String in) {
        if (in == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        char[] chars = in.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '§' && i + 1 < chars.length) {
                char c = Character.toLowerCase(chars[i + 1]);
                Integer ansiCode = CODE_TO_ANSI.get(c);
                if (ansiCode != null) {
                    sb.append("\u001B[").append(ansiCode).append("m");
                    i++;
                    continue;
                }
            }
            sb.append(chars[i]);
        }

        return sb.toString();
    }
}
