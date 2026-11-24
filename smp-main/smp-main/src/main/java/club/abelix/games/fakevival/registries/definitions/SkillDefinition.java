package club.abelix.games.fakevival.registries.definitions;

import java.util.Map;

public record SkillDefinition(String id, String displayName, int xpPerAction, boolean enabled,
                              Map<String, Object> data) {
    public SkillDefinition {
        data = Map.copyOf(data);
    }
}
