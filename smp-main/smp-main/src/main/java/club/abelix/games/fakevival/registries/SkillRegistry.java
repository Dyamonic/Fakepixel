package club.abelix.games.fakevival.registries;

import club.abelix.games.fakevival.config.StaticConfigDefaults;
import club.abelix.games.fakevival.registries.definitions.SkillDefinition;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SkillRegistry extends StaticConfigRegistry {
    private Map<String, SkillDefinition> skills = Map.of();

    public SkillRegistry(JavaPlugin plugin) {
        super(StaticConfigDefaults.SKILLS);
        reload(plugin);
    }

    @Override
    protected void rebuild() {
        Map<String, SkillDefinition> nextSkills = new LinkedHashMap<>();
        for (String skillId : skillIds()) {
            ConfigurationSection section = section("skills.definitions." + skillId);
            Map<String, Object> rawData = section == null ? Map.of() : data(section);
            nextSkills.put(skillId, new SkillDefinition(
                    skillId,
                    section == null ? skillId : displayName(section, skillId),
                    config().getInt("skills.xp-per-action." + skillId, 0),
                    section == null || section.getBoolean("enabled", true),
                    rawData
            ));
        }
        skills = Map.copyOf(nextSkills);
    }

    public List<String> skillIds() {
        return stringList("skills.enabled-skills");
    }

    public Optional<SkillDefinition> get(String id) {
        return Optional.ofNullable(skills.get(id));
    }

    public List<SkillDefinition> all() {
        return List.copyOf(skills.values());
    }

    public boolean contains(String id) {
        return skills.containsKey(id);
    }
}
