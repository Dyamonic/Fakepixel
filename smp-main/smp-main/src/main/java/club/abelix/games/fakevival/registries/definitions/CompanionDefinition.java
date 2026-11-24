package club.abelix.games.fakevival.registries.definitions;

import java.util.Map;

public record CompanionDefinition(String id, String displayName, boolean enabled,
                                  Map<String, Object> data) {
    public CompanionDefinition {
        data = Map.copyOf(data);
    }
}
