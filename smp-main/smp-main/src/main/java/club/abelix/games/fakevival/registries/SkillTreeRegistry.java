package club.abelix.games.fakevival.registries;

import club.abelix.games.fakevival.config.StaticConfigDefaults;
import club.abelix.games.fakevival.registries.definitions.SkillTreeNodeDefinition;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class SkillTreeRegistry extends StaticConfigRegistry {
    private Map<String, SkillTreeNodeDefinition> nodes = Map.of();
    private Map<String, List<SkillTreeNodeDefinition>> bySkill = Map.of();

    public SkillTreeRegistry(JavaPlugin plugin) {
        super(StaticConfigDefaults.SKILL_TREES);
        reload(plugin);
    }

    @Override
    protected void rebuild() {
        Map<String, SkillTreeNodeDefinition> nextNodes = new LinkedHashMap<>();
        Map<String, List<SkillTreeNodeDefinition>> nextBySkill = new LinkedHashMap<>();
        for (String skillId : treeIds()) {
            for (String nodeId : nodeIds(skillId)) {
                ConfigurationSection section = section("skills.trees." + skillId + "." + nodeId);
                if (section == null) continue;
                String id = skillId + "." + nodeId;
                SkillTreeNodeDefinition definition = new SkillTreeNodeDefinition(
                        id,
                        skillId,
                        displayName(section, nodeId),
                        section.getBoolean("enabled", true),
                        data(section)
                );
                nextNodes.put(id, definition);
                nextBySkill.computeIfAbsent(skillId, ignored -> new ArrayList<>()).add(definition);
            }
        }
        nodes = Map.copyOf(nextNodes);
        bySkill = immutableGrouped(nextBySkill);
    }

    public Set<String> treeIds() {
        return keys("skills.trees");
    }

    public Set<String> nodeIds(String skillId) {
        return keys("skills.trees." + skillId);
    }

    public Optional<SkillTreeNodeDefinition> get(String id) {
        return Optional.ofNullable(nodes.get(id));
    }

    public List<SkillTreeNodeDefinition> all() {
        return List.copyOf(nodes.values());
    }

    public boolean contains(String id) {
        return nodes.containsKey(id);
    }

    public List<SkillTreeNodeDefinition> bySkill(String skillId) {
        return bySkill.getOrDefault(skillId, List.of());
    }
}
