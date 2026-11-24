package club.abelix.games.fakevival.registries.definitions;

import java.util.Map;

public record SkillTreeNodeDefinition(String id, String skillId, String displayName, boolean enabled,
                                      Map<String, Object> data) {
    public SkillTreeNodeDefinition {
        data = Map.copyOf(data);
    }
}
