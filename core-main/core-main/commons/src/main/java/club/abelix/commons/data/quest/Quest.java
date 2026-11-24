package club.abelix.commons.data.quest;

import club.abelix.commons.data.stat.StatsMode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class Quest {

    private final int day;
    private final StatsMode mode;
    private final List<QuestRequirement> requirements;

    public int getCoinReward() {
        return requirements.stream()
                .mapToInt(QuestRequirement::getCoins)
                .sum();
    }

    public int getExperienceReward() {
        return requirements.stream()
                .mapToInt(QuestRequirement::getExperience)
                .sum();
    }

}
