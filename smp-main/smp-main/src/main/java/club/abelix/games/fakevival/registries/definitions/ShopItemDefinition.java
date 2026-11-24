package club.abelix.games.fakevival.registries.definitions;

import java.util.Map;

public record ShopItemDefinition(String id, String shopId, String displayName, String category, boolean enabled,
                                 Map<String, Object> data) {
    public ShopItemDefinition {
        data = Map.copyOf(data);
    }
}
