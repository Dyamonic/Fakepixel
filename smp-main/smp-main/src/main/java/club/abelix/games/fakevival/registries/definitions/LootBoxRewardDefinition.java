package club.abelix.games.fakevival.registries.definitions;

import java.util.Map;

public record LootBoxRewardDefinition(String id, String boxId, String displayName, String rarity, boolean enabled,
                                      Map<String, Object> data) {
    public LootBoxRewardDefinition {
        data = Map.copyOf(data);
    }
}
