package club.abelix.commons.slf4j;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import lombok.val;

public class LevelColorConverter extends ClassicConverter {

    private static final String RESET = "\u001B[0m";

    @Override
    public String convert(ILoggingEvent event) {
        val level = event.getLevel();
        val ansiColor = switch (level.toInt()) {
            case Level.ERROR_INT -> "\u001B[31m"; // красный
            case Level.WARN_INT -> "\u001B[33m"; // жёлтый
            case Level.INFO_INT -> "\u001B[32m"; // зелёный
            case Level.DEBUG_INT -> "\u001B[36m"; // голубой
            default -> "\u001B[37m"; // белый (TRACE и прочее)
        };

        return ansiColor + level + RESET;
    }
}
