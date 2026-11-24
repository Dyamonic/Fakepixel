package club.abelix.games.fakevival.registries.definitions;

import java.util.Map;

public record PlaytimeRewardDefinition(String id, String rewardSetId, int day, String displayName, boolean enabled,
                                       Map<String, Object> data) {
    public PlaytimeRewardDefinition {
        data = Map.copyOf(data);
    }
}
