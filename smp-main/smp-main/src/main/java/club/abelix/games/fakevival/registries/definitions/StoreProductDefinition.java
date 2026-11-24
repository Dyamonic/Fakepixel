package club.abelix.games.fakevival.registries.definitions;

import java.util.Map;

public record StoreProductDefinition(String id, String category, String displayName, boolean enabled,
                                     Map<String, Object> data) {
    public StoreProductDefinition {
        data = Map.copyOf(data);
    }
}
