package club.abelix.commons.data.dailyrewards;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class DailyRewardEntry {

    private final int playerId;
    private final int dayStart;

    private final int lastDay;

    public DailyRewardEntry changeRewardDate(int dayStart, int lastDay) {
        return new DailyRewardEntry(playerId, dayStart, lastDay);
    }

    public DailyRewardEntry changeRewardDate(int lastDay) {
        return changeRewardDate(dayStart, lastDay);
    }
}
