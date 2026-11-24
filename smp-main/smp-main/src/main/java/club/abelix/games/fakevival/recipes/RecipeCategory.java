package club.abelix.games.fakevival.recipes;

import org.bukkit.Material;

import java.util.Locale;

public enum RecipeCategory {
    WEAPONS("Weapons", Material.DIAMOND_SWORD),
    ARMOR("Armor", Material.DIAMOND_CHESTPLATE),
    ACCESSORIES("Accessories", Material.EMERALD),
    TOOLS("Tools", Material.IRON_PICKAXE),
    FISHING_EQUIPMENT("Fishing", Material.FISHING_ROD),
    FARMING_EQUIPMENT("Farming Equipment", Material.GOLDEN_HOE),
    POTIONS("Potions", Material.POTION),
    FOOD("Food", Material.BREAD),
    MAGIC("Magic", Material.BLAZE_ROD),
    DUNGEON("Dungeon", Material.ECHO_SHARD),
    UTILITY("Utility", Material.CRAFTING_TABLE),
    MATERIALS("Materials", Material.AMETHYST_SHARD),
    VANILLA("Vanilla", Material.GRASS_BLOCK);

    private final String displayName;
    private final Material icon;

    RecipeCategory(String displayName, Material icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String displayName() {
        return displayName;
    }

    public Material icon() {
        return icon;
    }

    public static RecipeCategory parse(String value) {
        if (value == null || value.isBlank()) return UTILITY;
        try {
            String normalized = value.toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
            if (normalized.equals("FARMING")) normalized = "FARMING_EQUIPMENT";
            if (normalized.equals("MINING")) normalized = "TOOLS";
            if (normalized.equals("COMBAT")) normalized = "WEAPONS";
            if (normalized.equals("FORAGING")) normalized = "TOOLS";
            if (normalized.equals("FISHING")) normalized = "FISHING_EQUIPMENT";
            if (normalized.equals("SACKS")) normalized = "UTILITY";
            if (normalized.equals("SWORDS") || normalized.equals("DAGGERS") || normalized.equals("KATANAS") || normalized.equals("BOWS")) normalized = "WEAPONS";
            if (normalized.equals("WANDS") || normalized.equals("STAFFS") || normalized.equals("WANDS_STAFFS")) normalized = "MAGIC";
            if (normalized.equals("RINGS") || normalized.equals("CHARMS") || normalized.equals("TOTEMS") || normalized.equals("NECKLACES")) normalized = "ACCESSORIES";
            return RecipeCategory.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return UTILITY;
        }
    }
}
