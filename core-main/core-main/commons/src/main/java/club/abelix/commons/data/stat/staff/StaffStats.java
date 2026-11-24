package club.abelix.commons.data.stat.staff;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@AllArgsConstructor
@Data
public class StaffStats {

    public final static StaffStats EMPTY = new StaffStats(0, 0, LocalDateTime.now());

    private int closedReposts;
    private int punishmentReports;
    private LocalDateTime nextUpdate;

    public void addClosedReports(int value) {
        closedReposts += value;
    }

    public void addPunishmentReports(int value) {
        punishmentReports += value;
    }
}
