package club.abelix.games.fakevival.registries.definitions;

import java.util.Map;

public record BazaarItemDefinition(String id, String displayName, String category, boolean enabled,
                                   Map<String, Object> data) {
    public BazaarItemDefinition {
        data = Map.copyOf(data);
    }
}
