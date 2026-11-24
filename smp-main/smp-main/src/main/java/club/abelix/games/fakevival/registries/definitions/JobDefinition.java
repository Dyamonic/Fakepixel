package club.abelix.games.fakevival.registries.definitions;

import java.util.List;
import java.util.Map;

public record JobDefinition(String id, String displayName, List<String> actions, int xp, boolean enabled,
                            Map<String, Object> data) {
    public JobDefinition {
        actions = List.copyOf(actions);
        data = Map.copyOf(data);
    }
}
