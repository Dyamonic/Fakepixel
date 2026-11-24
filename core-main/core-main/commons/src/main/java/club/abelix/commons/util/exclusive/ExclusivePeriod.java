package club.abelix.commons.util.exclusive;

import club.abelix.commons.language.Language;
import club.abelix.commons.util.color.ChatColor;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public enum ExclusivePeriod {

    /**
     * Дату окончания события надо указывать следующим днем, а дату начала
     * - предыдущим, от реального дня начала и конца соответственно
     */

    EASTER(ChatColor.AQUA, "period_easter", 30, 29, Month.MARCH, Month.APRIL),
    SUMMER(ChatColor.YELLOW, "period_summer", 15, 9, Month.JUNE, Month.SEPTEMBER),
    HALLOWEEN(ChatColor.DARK_PURPLE, "period_halloween", 2, 6, Month.OCTOBER, Month.NOVEMBER),
    NEW_YEAR(ChatColor.AQUA, "period_new_year", 16, 1, Month.DECEMBER, Month.FEBRUARY);

    public static final ExclusivePeriod[] VALUES = ExclusivePeriod.values();

    // -----------------------------------------------------------------------

    /**
     * Цвет для этого типа предметов
     */
    private final ChatColor color;

    /**
     * Эксклюзивное имя
     */
    private final String exclusiveKey;

    /**
     * Тут по датам окончания этого события
     * Чтобы после них нельзя было выбрать этот предмет
     */
    private final int startDayOfMonth, endDayOfMonth;

    /**
     * Тут по месяцам окончания этого события
     * Между startMonth & endMonth можно будет выбирать
     */
    private final Month startMonth, endMonth;

    public static @NotNull List<ExclusivePeriod> getCurrentExclusivePeriods() {
        List<ExclusivePeriod> periods = new ArrayList<>();

        for (ExclusivePeriod period : VALUES) {
            if (!period.isAvailable()) {
                continue;
            }

            periods.add(period);
        }

        return periods;
    }

    public static @Nullable ExclusivePeriod getCurrentExclusivePeriod() {
        for (ExclusivePeriod period : VALUES) {
            if (!period.isAvailable()) {
                continue;
            }

            return period;
        }

        return null;
    }

    public boolean isAvailable() {
        LocalDate currentDate = LocalDate.now();

        return currentDate.isAfter(getStartDate().plusDays(1))
                && currentDate.isBefore(getEndDate().plusDays(1));
    }

    public LocalDate getStartDate() {
        LocalDate currentDate = LocalDate.now();

        int year = currentDate.getYear();

        if (startMonth.getValue() > endMonth.getValue()
                && startMonth.getValue() > currentDate.getMonth().getValue()) {
            year -= 1;
        }

        return LocalDate.of(year, startMonth, startDayOfMonth);
    }

    public LocalDate getEndDate() {
        LocalDate currentDate = LocalDate.now();
        LocalDate startDate = getStartDate();
        // проверяем, если месяц окончания доступа к предмету меньше чем месяц старта
        // это означает, что доступ заканчивается в следующем году
        // в этом случае будут считаться все месяцы ДО конца года, и потом все сначала до endMonth
        boolean nextYear = startMonth.getValue() > endMonth.getValue() && startDate.getYear() == currentDate.getYear();


        return LocalDate.of(currentDate.getYear() + (nextYear ? 1 : 0), endMonth, endDayOfMonth);
    }

    public String getExclusiveName(Language language) {
        return language.getMessage(exclusiveKey);
    }

    public String getDisplayName(Language language) {
        return color.toString() + getExclusiveName(language);
    }

    public String getExclusiveIdent() {
        return this.name().toLowerCase();
    }

    @Contract(pure = true)
    @Override
    public @NotNull String toString() {
        return "ExclusivePeriod/" + this.name();
    }

}
