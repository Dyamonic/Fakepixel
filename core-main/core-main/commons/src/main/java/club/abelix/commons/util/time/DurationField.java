package club.abelix.commons.util.time;

import java.util.concurrent.TimeUnit;

/**
 * Created by saharNooby on 14:27 06.10.2017
 */
public enum DurationField {

    NANO(TimeUnit.NANOSECONDS),
    MICRO(TimeUnit.MICROSECONDS),
    MILLI(TimeUnit.MILLISECONDS),

    SEC(TimeUnit.SECONDS),
    MIN(TimeUnit.MINUTES),
    HOUR(TimeUnit.HOURS),

    DAY(TimeUnit.DAYS),
    WEEK(TimeUnit.DAYS.toNanos(7)),
    MONTH(TimeUnit.DAYS.toNanos(30)),
    YEAR(TimeUnit.DAYS.toNanos(365));

    final long nanos;

    DurationField(TimeUnit unit) {
        this(unit.toNanos(1));
    }

    DurationField(long nanos) {
        this.nanos = nanos;
    }

}
