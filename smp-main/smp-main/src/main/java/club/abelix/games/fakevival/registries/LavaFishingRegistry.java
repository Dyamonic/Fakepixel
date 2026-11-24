package club.abelix.games.fakevival.registries;

import club.abelix.games.fakevival.registries.definitions.LavaFishingCatchDefinition;
import club.abelix.games.fakevival.registries.definitions.LavaFishingLootDefinition;
import club.abelix.games.fakevival.registries.definitions.LavaFishingRewardDefinition;

import java.util.List;
import java.util.Optional;

public final class LavaFishingRegistry {
    private final LavaFishingCatchDefinition catchDefinition = LavaFishingDefaults.catchDefaults();
    private final List<LavaFishingRewardDefinition> customRewards = List.copyOf(LavaFishingDefaults.customRewards());
    private final List<LavaFishingLootDefinition> loot = List.copyOf(LavaFishingDefaults.loot());

    public LavaFishingCatchDefinition catchDefinition() {
        return catchDefinition;
    }

    public List<LavaFishingRewardDefinition> customRewards() {
        return customRewards;
    }

    public List<LavaFishingLootDefinition> loot() {
        return loot;
    }

    public Optional<LavaFishingLootDefinition> loot(String id) {
        return loot.stream().filter(definition -> definition.id().equalsIgnoreCase(id)).findFirst();
    }
}
