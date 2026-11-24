package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;

public final class ItemRarityDefaults {
    private ItemRarityDefaults() {
    }

    public static void apply(ConfigurationSection root) {
        root.set("item-rarity.enabled", true);
        root.set("item-rarity.apply-to", "CUSTOM_ITEMS");
        root.set("item-rarity.include-stackable-items", false);
        root.set("item-rarity.apply-to-existing-items-on-join", true);
        root.set("item-rarity.cleanup-invalid-rarity-lore", true);
        root.set("item-rarity.excluded-materials", List.of("AIR"));
        root.set("item-rarity.rarities.common.display", "COMMON");
        root.set("item-rarity.rarities.common.color", "&f");
        root.set("item-rarity.rarities.common.weight", 600);
        root.set("item-rarity.rarities.uncommon.display", "UNCOMMON");
        root.set("item-rarity.rarities.uncommon.color", "&a");
        root.set("item-rarity.rarities.uncommon.weight", 250);
        root.set("item-rarity.rarities.rare.display", "RARE");
        root.set("item-rarity.rarities.rare.color", "&9");
        root.set("item-rarity.rarities.rare.weight", 100);
        root.set("item-rarity.rarities.epic.display", "EPIC");
        root.set("item-rarity.rarities.epic.color", "&5");
        root.set("item-rarity.rarities.epic.weight", 40);
        root.set("item-rarity.rarities.legendary.display", "LEGENDARY");
        root.set("item-rarity.rarities.legendary.color", "&6");
        root.set("item-rarity.rarities.legendary.weight", 9);
        root.set("item-rarity.rarities.mythic.display", "MYTHIC");
        root.set("item-rarity.rarities.mythic.color", "&d");
        root.set("item-rarity.rarities.mythic.weight", 1);
    }
}
