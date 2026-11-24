package club.abelix.commons.util.date;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Locale;

/**
 * @author milansky
 */
@UtilityClass
public final class DateUtility {

    private final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd MMM yyyy 'г.,' HH:mm:ss", new Locale("ru"));

    public String convert(@NonNull Date date) {
        return DATE_FORMAT.format(date);
    }

    public String convert(@NonNull LocalDateTime dateTime) {
        ZonedDateTime zonedDateTime = dateTime.atZone(ZoneId.systemDefault());
        Date date = Date.from(zonedDateTime.toInstant());
        return DATE_FORMAT.format(date);
    }

    public String convert(long time) {
        return DATE_FORMAT.format(new Date(time));
    }


}
