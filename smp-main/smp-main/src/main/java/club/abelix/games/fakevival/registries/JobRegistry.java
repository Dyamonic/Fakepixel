package club.abelix.games.fakevival.registries;

import club.abelix.games.fakevival.config.StaticConfigDefaults;
import club.abelix.games.fakevival.registries.definitions.JobDefinition;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class JobRegistry extends StaticConfigRegistry {
    private Map<String, JobDefinition> jobs = Map.of();

    public JobRegistry(JavaPlugin plugin) {
        super(StaticConfigDefaults.JOBS);
        reload(plugin);
    }

    @Override
    protected void rebuild() {
        Map<String, JobDefinition> nextJobs = new LinkedHashMap<>();
        for (String jobId : jobIds()) {
            ConfigurationSection section = section("jobs." + jobId);
            if (section == null) continue;
            nextJobs.put(jobId, new JobDefinition(
                    jobId,
                    displayName(section, jobId),
                    section.getStringList("actions"),
                    section.getInt("xp", section.getInt("xp-per-action", 0)),
                    section.getBoolean("enabled", true),
                    data(section)
            ));
        }
        jobs = Map.copyOf(nextJobs);
    }

    public Set<String> jobIds() {
        return keys("jobs");
    }

    public Optional<JobDefinition> get(String id) {
        return Optional.ofNullable(jobs.get(id));
    }

    public List<JobDefinition> all() {
        return List.copyOf(jobs.values());
    }

    public boolean contains(String id) {
        return jobs.containsKey(id);
    }
}
