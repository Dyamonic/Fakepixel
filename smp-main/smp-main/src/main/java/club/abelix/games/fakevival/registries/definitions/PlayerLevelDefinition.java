package club.abelix.games.fakevival.registries.definitions;

import java.util.Map;

public record PlayerLevelDefinition(String id, int level, String displayName, boolean enabled,
                                    Map<String, Object> data) {
    public PlayerLevelDefinition {
        data = Map.copyOf(data);
    }
}
