package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Java-only defaults for the Futuristic armor set and its progression recipes. */
public final class FuturisticArmorDefaults {
    public static final String SET_ID = "futuristic";
    public static final String HEAD64 = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTcyMDVkZDVmZjJlZGYxNjc2YTBkNTdmN2EwOWEyODU5NWVmNjVlZGQxZTUzMDU2YzYzZWEzNGM0NzYzNzRhZCJ9fX0=";
    private static final Set<String> RECIPE_IDS = Set.of("futuristic_helmet_recipe", "futuristic_chestplate_recipe", "futuristic_leggings_recipe", "futuristic_boots_recipe");

    private FuturisticArmorDefaults() {}

    public static void apply(ConfigurationSection root) {
        material(root, "enchanted_netherite_ingot", "&5Enchanted Netherite Ingot", "NETHERITE_INGOT", "EPIC");
        material(root, "enchanted_blaze_powder", "&aEnchanted Blaze Powder", "BLAZE_POWDER", "RARE");
        if (!root.isConfigurationSection("custom-items.items.enchanted_iron_block"))
            material(root, "enchanted_iron_block", "&9Enchanted Iron Block", "IRON_BLOCK", "RARE");
        armor(root, "enchanted_diamond_helmet", "&bEnchanted Diamond Helmet", "DIAMOND_HELMET", "HELMET", List.of("&7An enchanted helmet used in", "&7advanced armor construction."));
        root.set("custom-items.items.enchanted_diamond_helmet.armor-set-id", null);
        root.set("custom-items.items.enchanted_diamond_helmet.armor-piece", null);

        armor(root, "futuristic_helmet", "&bFuturistic Helmet", "PLAYER_HEAD", "HELMET", List.of(
                "&7A high-tech helmet made with", "&7advanced futuristic plating.", "", "&8Piece: Helmet", "&8Set: Futuristic", "",
                "&6Full Set Bonus: &bFuture Defense", "&7Gain &c+20 HP &7and &a+15 Defense&7.", "", "&5&lEPIC HELMET"));
        root.set("custom-items.items.futuristic_helmet.head64", HEAD64);
        armor(root, "futuristic_chestplate", "&bFuturistic Chestplate", "LEATHER_CHESTPLATE", "CHESTPLATE", pieceLore("Chestplate", "CHESTPLATE"));
        armor(root, "futuristic_leggings", "&bFuturistic Leggings", "LEATHER_LEGGINGS", "LEGGINGS", pieceLore("Leggings", "LEGGINGS"));
        armor(root, "futuristic_boots", "&bFuturistic Boots", "LEATHER_BOOTS", "BOOTS", pieceLore("Boots", "BOOTS"));
        for (String id : List.of("futuristic_chestplate", "futuristic_leggings", "futuristic_boots"))
            root.set("custom-items.items." + id + ".leather-color", "0,180,255");

        recipe(root, "futuristic_helmet_recipe", "&bFuturistic Helmet", "futuristic_helmet",
                Map.of('A', "custom:enchanted_netherite_ingot", 'B', "custom:enchanted_iron_block", 'C', "custom:enchanted_blaze_powder", 'D', "custom:enchanted_diamond_helmet"),
                Map.of('A', 2, 'B', 3, 'C', 2, 'D', 1));
        recipe(root, "futuristic_chestplate_recipe", "&bFuturistic Chestplate", "futuristic_chestplate",
                Map.of('A', "custom:enchanted_netherite_ingot", 'B', "custom:enchanted_iron_block", 'C', "custom:enchanted_blaze_powder", 'D', "material:LEATHER_CHESTPLATE"),
                Map.of('A', 3, 'B', 4, 'C', 2, 'D', 1));
        recipe(root, "futuristic_leggings_recipe", "&bFuturistic Leggings", "futuristic_leggings",
                Map.of('A', "custom:enchanted_netherite_ingot", 'B', "custom:enchanted_iron_block", 'C', "custom:enchanted_blaze_powder", 'D', "material:LEATHER_LEGGINGS"),
                Map.of('A', 2, 'B', 4, 'C', 2, 'D', 1));
        recipe(root, "futuristic_boots_recipe", "&bFuturistic Boots", "futuristic_boots",
                Map.of('A', "custom:enchanted_netherite_ingot", 'B', "custom:enchanted_iron_block", 'C', "custom:enchanted_blaze_powder", 'D', "material:LEATHER_BOOTS"),
                Map.of('A', 1, 'B', 2, 'C', 1, 'D', 1));
    }

    public static Set<String> recipeIds() { return RECIPE_IDS; }

    private static void material(ConfigurationSection root, String id, String name, String material, String rarity) {
        String p = "custom-items.items." + id;
        if (root.isConfigurationSection(p)) return;
        root.set(p + ".name", name); root.set(p + ".material", material); root.set(p + ".type", "RECIPE_MATERIAL");
        root.set(p + ".mmo.type", "RECIPE_MATERIAL"); root.set(p + ".category", "materials"); root.set(p + ".rarity", rarity);
        root.set(p + ".lore", List.of("&7An enchanted material used in", "&7advanced crafting recipes.")); root.set(p + ".mmo.random-roll", false);
        root.set(p + ".ability.type", "none"); root.set(p + ".max-stack-size", 64);
    }

    private static void armor(ConfigurationSection root, String id, String name, String material, String piece, List<String> lore) {
        String p = "custom-items.items." + id;
        root.set(p + ".name", name); root.set(p + ".material", material); root.set(p + ".type", "ARMOR"); root.set(p + ".mmo.type", "ARMOR");
        root.set(p + ".category", "armor"); root.set(p + ".rarity", "EPIC"); root.set(p + ".lore", lore); root.set(p + ".unbreakable", true);
        root.set(p + ".fixed-rarity", true);
        root.set(p + ".mmo.random-roll", true); root.set(p + ".mmo.stats.HEALTH", 35); root.set(p + ".mmo.stats.DEFENSE", 25);
        root.set(p + ".armor-set-id", SET_ID); root.set(p + ".armor-piece", piece); root.set(p + ".ability.type", "none");
    }

    private static List<String> pieceLore(String piece, String type) { return List.of(
            "&7High-tech armor built with", "&7reinforced futuristic materials.", "", "&8Piece: " + piece, "&8Set: Futuristic", "",
            "&6Full Set Bonus: &bFuture Defense", "&7Gain &c+20 HP &7and &a+15 Defense&7.", "", "&5&lEPIC " + type); }

    private static void recipe(ConfigurationSection root, String id, String name, String result, Map<Character, String> ingredients, Map<Character, Integer> amounts) {
        String p = "recipes." + id;
        root.set(p + ".display-name", name); root.set(p + ".category", "ARMOR"); root.set(p + ".result", result);
        root.set(p + ".shape", List.of("ABC", " D ", "   ")); root.set(p + ".unlock.type", "MULTI_COLLECTION");
        root.set(p + ".unlock.requirements.netherite", 4); root.set(p + ".unlock.requirements.iron", 5); root.set(p + ".unlock.requirements.blaze_rod", 4);
        ingredients.forEach((key, value) -> root.set(p + ".ingredients." + key, value));
        amounts.forEach((key, value) -> root.set(p + ".amounts." + key, value));
        for (Map.Entry<String, Integer> requirement : Map.of("netherite", 4, "iron", 5, "blaze_rod", 4).entrySet()) {
            String unlock = "collections.items." + requirement.getKey() + ".unlocks." + requirement.getValue();
            if (!root.isConfigurationSection(unlock)) { root.set(unlock + ".amount", requirement.getValue() == 5 ? 1000 : 100); root.set(unlock + ".reward-type", "recipe"); root.set(unlock + ".reward-value", id); root.set(unlock + ".name", name); }
        }
    }
}
