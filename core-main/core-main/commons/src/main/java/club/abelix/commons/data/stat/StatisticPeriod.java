package club.abelix.commons.data.stat;

import club.abelix.commons.language.Language;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

@RequiredArgsConstructor
@Getter
public enum StatisticPeriod {

    // Период статистики в днях

    ALL,
    MONTH {
        @Override
        public int getFromDay() {
            LocalDate firstMonthDate = LocalDate.now().withDayOfMonth(1);

            return (int) firstMonthDate.toEpochDay();
        }
    },
    WEEK {
        @Override
        public int getFromDay() {
            LocalDateTime time = LocalDateTime.now();
            LocalDate date = time.toLocalDate();

            if (time.getDayOfWeek() == DayOfWeek.MONDAY) {
                if (time.isAfter(time.withHour(12).withMinute(0))) {
                    return (int) date.toEpochDay();
                } else {
                    return (int) date.with(TemporalAdjusters.previous(DayOfWeek.MONDAY))
                            .toEpochDay();
                }
            }

            return (int) date.with(DayOfWeek.MONDAY).toEpochDay();
        }
    },
    TODAY {
        @Override
        public int getFromDay() {
            return (int) LocalDate.now().toEpochDay();
        }
    };

    public String getDisplayName(@NonNull Language language) {
        return language.getMessage(name() + "_player_top_name");
    }

    public int getFromDay() {
        return 0;
    }

}
