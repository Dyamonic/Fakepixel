package club.abelix.games.fakevival.registries;

import club.abelix.games.fakevival.config.StaticConfigDefaults;
import club.abelix.games.fakevival.registries.definitions.BattlepassTierDefinition;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class BattlepassRegistry extends StaticConfigRegistry {
    private Map<String, BattlepassTierDefinition> tiers = Map.of();

    public BattlepassRegistry(JavaPlugin plugin) {
        super(StaticConfigDefaults.BATTLEPASS);
        reload(plugin);
    }

    @Override
    protected void rebuild() {
        Map<String, BattlepassTierDefinition> nextTiers = new LinkedHashMap<>();
        for (String tierId : tierIds()) {
            ConfigurationSection tier = section("rewards." + tierId);
            if (tier == null) continue;
            nextTiers.put(tierId, new BattlepassTierDefinition(
                    tierId,
                    displayName(tier, tierId),
                    tier.getBoolean("enabled", true),
                    data(tier)
            ));
        }
        tiers = Map.copyOf(nextTiers);
    }

    public Set<String> tierIds() {
        return keys("rewards");
    }

    public Optional<BattlepassTierDefinition> get(String id) {
        return Optional.ofNullable(tiers.get(id));
    }

    public List<BattlepassTierDefinition> all() {
        return List.copyOf(tiers.values());
    }

    public boolean contains(String id) {
        return tiers.containsKey(id);
    }
}
