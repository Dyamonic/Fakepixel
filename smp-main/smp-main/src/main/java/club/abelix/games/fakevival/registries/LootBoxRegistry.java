package club.abelix.games.fakevival.registries;

import club.abelix.games.fakevival.config.StaticConfigDefaults;
import club.abelix.games.fakevival.registries.definitions.LootBoxRewardDefinition;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class LootBoxRegistry extends StaticConfigRegistry {
    private Map<String, LootBoxRewardDefinition> rewards = Map.of();
    private Map<String, List<LootBoxRewardDefinition>> byBox = Map.of();

    public LootBoxRegistry(JavaPlugin plugin) {
        super(StaticConfigDefaults.LOOT_BOXES);
        reload(plugin);
    }

    @Override
    protected void rebuild() {
        Map<String, LootBoxRewardDefinition> nextRewards = new LinkedHashMap<>();
        Map<String, List<LootBoxRewardDefinition>> nextByBox = new LinkedHashMap<>();
        for (String boxId : boxIds()) {
            ConfigurationSection rewardsSection = section("loot-boxes.boxes." + boxId + ".rewards");
            if (rewardsSection == null) continue;
            for (String rewardId : rewardsSection.getKeys(false)) {
                ConfigurationSection reward = rewardsSection.getConfigurationSection(rewardId);
                if (reward == null) continue;
                String id = boxId + "." + rewardId;
                LootBoxRewardDefinition definition = new LootBoxRewardDefinition(
                        id,
                        boxId,
                        displayName(reward, rewardId),
                        reward.getString("rarity", "common"),
                        reward.getBoolean("enabled", true),
                        data(reward)
                );
                nextRewards.put(id, definition);
                nextByBox.computeIfAbsent(boxId, ignored -> new ArrayList<>()).add(definition);
            }
        }
        rewards = Map.copyOf(nextRewards);
        byBox = immutableGrouped(nextByBox);
    }

    public Set<String> boxIds() {
        return keys("loot-boxes.boxes");
    }

    public Optional<LootBoxRewardDefinition> get(String id) {
        return Optional.ofNullable(rewards.get(id));
    }

    public List<LootBoxRewardDefinition> all() {
        return List.copyOf(rewards.values());
    }

    public boolean contains(String id) {
        return rewards.containsKey(id);
    }

    public List<LootBoxRewardDefinition> byBox(String boxId) {
        return byBox.getOrDefault(boxId, List.of());
    }
}
