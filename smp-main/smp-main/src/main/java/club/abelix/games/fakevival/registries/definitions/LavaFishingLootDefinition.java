package club.abelix.games.fakevival.registries.definitions;

import org.bukkit.Material;

public record LavaFishingLootDefinition(String id, Material material, int minAmount, int maxAmount, double weight,
                                        String displayName) {
}
