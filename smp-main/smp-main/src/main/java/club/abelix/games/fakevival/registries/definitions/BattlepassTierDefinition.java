package club.abelix.games.fakevival.registries.definitions;

import java.util.Map;

public record BattlepassTierDefinition(String id, String displayName, boolean enabled,
                                       Map<String, Object> data) {
    public BattlepassTierDefinition {
        data = Map.copyOf(data);
    }
}
