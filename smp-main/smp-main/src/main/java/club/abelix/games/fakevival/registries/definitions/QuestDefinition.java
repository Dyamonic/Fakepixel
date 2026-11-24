package club.abelix.games.fakevival.registries.definitions;

import java.util.Map;

public record QuestDefinition(String id, String type, String displayName, boolean enabled,
                              Map<String, Object> data) {
    public QuestDefinition {
        data = Map.copyOf(data);
    }
}
