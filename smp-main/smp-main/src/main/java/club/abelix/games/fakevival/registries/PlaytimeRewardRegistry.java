package club.abelix.games.fakevival.registries;

import club.abelix.games.fakevival.config.StaticConfigDefaults;
import club.abelix.games.fakevival.registries.definitions.PlaytimeRewardDefinition;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class PlaytimeRewardRegistry extends StaticConfigRegistry {
    private Map<String, PlaytimeRewardDefinition> rewards = Map.of();
    private Map<String, List<PlaytimeRewardDefinition>> byRewardSet = Map.of();

    public PlaytimeRewardRegistry(JavaPlugin plugin) {
        super(StaticConfigDefaults.PLAYTIME_REWARDS);
        reload(plugin);
    }

    @Override
    protected void rebuild() {
        Map<String, PlaytimeRewardDefinition> nextRewards = new LinkedHashMap<>();
        Map<String, List<PlaytimeRewardDefinition>> nextByRewardSet = new LinkedHashMap<>();
        for (String setId : rewardSetIds()) {
            ConfigurationSection days = section("playtime-rewards.reward-sets." + setId + ".days");
            if (days == null) continue;
            for (String dayId : days.getKeys(false)) {
                ConfigurationSection reward = days.getConfigurationSection(dayId);
                if (reward == null) continue;
                String id = setId + "." + dayId;
                PlaytimeRewardDefinition definition = new PlaytimeRewardDefinition(
                        id,
                        setId,
                        parseDay(dayId),
                        displayName(reward, dayId),
                        reward.getBoolean("enabled", true),
                        data(reward)
                );
                nextRewards.put(id, definition);
                nextByRewardSet.computeIfAbsent(setId, ignored -> new ArrayList<>()).add(definition);
            }
        }
        rewards = Map.copyOf(nextRewards);
        byRewardSet = immutableGrouped(nextByRewardSet);
    }

    public Set<String> rewardSetIds() {
        return keys("playtime-rewards.reward-sets");
    }

    public Optional<PlaytimeRewardDefinition> get(String id) {
        return Optional.ofNullable(rewards.get(id));
    }

    public List<PlaytimeRewardDefinition> all() {
        return List.copyOf(rewards.values());
    }

    public boolean contains(String id) {
        return rewards.containsKey(id);
    }

    public List<PlaytimeRewardDefinition> byRewardSet(String setId) {
        return byRewardSet.getOrDefault(setId, List.of());
    }

    private int parseDay(String dayId) {
        try {
            return Integer.parseInt(dayId.replace("day_", ""));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
