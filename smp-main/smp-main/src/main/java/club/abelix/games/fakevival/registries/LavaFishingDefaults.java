package club.abelix.games.fakevival.registries;

import club.abelix.games.fakevival.registries.definitions.LavaFishingCatchDefinition;
import club.abelix.games.fakevival.registries.definitions.LavaFishingLootDefinition;
import club.abelix.games.fakevival.registries.definitions.LavaFishingRewardDefinition;
import org.bukkit.Material;

import java.util.List;

public final class LavaFishingDefaults {
    private LavaFishingDefaults() {
    }

    public static LavaFishingCatchDefinition catchDefaults() {
        return new LavaFishingCatchDefinition(
                true,
                true,
                "&6Lava Fishing &8> &fYou pulled something from the magma.",
                8.0D
        );
    }

    public static List<LavaFishingRewardDefinition> customRewards() {
        return List.of(
                new LavaFishingRewardDefinition("ember_shard", "ember_shard", 50.0D),
                new LavaFishingRewardDefinition("magma_core", "magma_core", 50.0D)
        );
    }

    public static List<LavaFishingLootDefinition> loot() {
        return List.of(
                new LavaFishingLootDefinition("magma_cream", Material.MAGMA_CREAM, 1, 3, 25.0D, "&6Lava Magma Cream"),
                new LavaFishingLootDefinition("blaze_powder", Material.BLAZE_POWDER, 1, 3, 25.0D, "&6Lava Blaze Powder"),
                new LavaFishingLootDefinition("fire_charge", Material.FIRE_CHARGE, 1, 1, 15.0D, "&6Lava Fire Charge"),
                new LavaFishingLootDefinition("nether_brick", Material.NETHER_BRICK, 1, 3, 20.0D, "&6Lava Nether Brick"),
                new LavaFishingLootDefinition("basalt", Material.BASALT, 1, 3, 15.0D, "&6Lava Basalt")
        );
    }
}
