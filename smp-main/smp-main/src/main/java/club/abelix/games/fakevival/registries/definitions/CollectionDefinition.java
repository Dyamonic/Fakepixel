package club.abelix.games.fakevival.registries.definitions;

import java.util.Map;

public record CollectionDefinition(String id, String displayName, boolean enabled,
                                   Map<String, Object> data) {
    public CollectionDefinition {
        data = Map.copyOf(data);
    }
}
