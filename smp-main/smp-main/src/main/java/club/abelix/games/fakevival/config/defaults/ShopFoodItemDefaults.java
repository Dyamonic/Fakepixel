package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Authoritative Java-backed item definitions used by cooking and food shops.
 *
 * <p>This pass intentionally runs after the general recipe/item defaults and before shop defaults. Some
 * recipe outputs share IDs with cooking ingredients, so merely registering an item when absent can leave
 * {@code dough}, {@code flour}, and similar entries classified as RECIPE_MATERIAL. These definitions force
 * the stable PDC-facing type and safety flags without relying on display names or lore.</p>
 */
public final class ShopFoodItemDefaults {
    private ShopFoodItemDefaults() {
    }

    private static final Set<String> REQUESTED_INGREDIENTS = orderedWords("""
            flour dough bread_dough sugar_powder salt pepper spice_mix hot_spice_mix butter cream cheese
            enchanted_flour enchanted_dough sweet_dough cookie_dough cake_batter pie_crust frosting
            chocolate_mix berry_jam apple_slices fish_fillet salmon_fillet cooked_fish_fillet
            grilled_salmon_fillet pufferfish_spice raw_beef_slice raw_pork_slice raw_chicken_slice
            raw_mutton_slice raw_rabbit_slice cooked_meat_slice golden_sauce carrot_slices potato_slices
            beetroot_slices pumpkin_puree melon_chunks melon_juice mushroom_slice cooked_mushroom_mix
            egg_mix mana_syrup milk_bucket_custom honey_glaze potato_starch beetroot_extract seaweed_wrap
            healing_herbs river_mint fern_leaf aloe_leaf nether_wart_spice enchanted_butter enchanted_cheese
            """);

    private static final Set<String> UNCOMMON_INGREDIENTS = Set.of(
            "spice_mix", "hot_spice_mix", "enchanted_flour", "enchanted_dough", "chocolate_mix",
            "berry_jam", "cooked_fish_fillet", "grilled_salmon_fillet", "pufferfish_spice",
            "cooked_meat_slice", "golden_sauce", "cooked_mushroom_mix", "mana_syrup");

    private static final Set<String> REQUESTED_FOODS = orderedWords("""
            campfire_toast sweet_campfire_roll campfire_fish_skewer campfire_beef_bites campfire_smores
            grand_campfire_feast ultimate_campfire_platter crispy_flatbread buttered_toast cheesy_toast
            garlic_flatbread honey_glazed_bread enchanted_bread_loaf royal_cheese_roll chocolate_cookie
            frosted_cookie apple_hand_pie pumpkin_spice_pie chocolate_cake_slice golden_honey_cake
            royal_bakers_feast crispy_cod_bites buttered_cod_plate smoked_fish_skewer fish_sandwich
            fishers_lunchbox peppered_salmon_plate honey_salmon_glaze seaweed_salmon_bowl salmon_seaweed_roll
            deep_sea_sushi royal_sushi_platter ocean_kings_feast beef_jerky pork_cracklings chicken_skewer
            mutton_spice_roast rabbit_herb_roast pepper_steak_plate butchers_feast smoked_pork_slices
            roasted_chicken_strips spicy_meat_plate golden_sauce_steak warrior_meat_stew smokers_feast
            roasted_carrot_bowl loaded_potato_bowl beetroot_cream_soup vegetable_stew farmer_stew
            golden_farm_platter farmers_lunchbox carrot_soup pumpkin_soup beetroot_stew_plus
            mushroom_stew_custom mushroom_bake hearty_garden_stew healers_garden_soup sweet_cream_drink
            honey_milk hot_chocolate mana_berry_tea healing_herb_tea spiced_mana_cider mana_frosted_cookie
            mana_cake_slice mana_cream_drink hot_mana_chocolate frosted_mana_drink mana_royal_feast
            healing_soup spiced_healing_tea golden_healing_drink arcane_feast_drink spicy_salmon_skewer
            puffer_spice_soup spicy_fish_stew spicy_cod_bowl spiced_flatbread fishermans_breakfast
            garden_breakfast hearty_breakfast_plate miners_lunch foragers_snack archers_ration mages_ration
            tank_ration furnace_feast blast_furnace_feast garden_royal_feast puffer_heat_stew
            tyrants_banquet soulfire_tea soul_campfire_feast storm_fisher_stew mana_smore royal_seafood_stew
            seared_steak_plate charred_pork_ribs crispy_chicken_bites fire_roasted_mutton
            volcanic_rabbit_skewer blast_baked_potato molten_cheese_bread lava_spiced_stew
            iron_pan_breakfast forge_grilled_cod forge_grilled_salmon crispy_kelp_chips blackstone_burger
            miners_lava_lunch scorched_mushroom_mix furnace_fried_fish blazing_salmon_roll
            golden_forge_feast nether_heat_platter royal_forge_banquet
            """);

    public static void apply(ConfigurationSection root) {
        // Register the established cooking definitions first; the normalization below is deliberately last.
        CookingDefaults.apply(root);
        CookingFoodDefaults40.apply(root);
        CookingFoodDefaults80.apply(root);
        CookingBlastDefaults.apply(root);

        for (String id : REQUESTED_INGREDIENTS) normalizeIngredient(root, id);

        Map<String, CookingFoodDefaults40.Food> definitions = new LinkedHashMap<>();
        CookingFoodDefaults40.foods().forEach(food -> definitions.put(food.id(), food));
        CookingFoodDefaults80.foods().forEach(food -> definitions.put(food.id(), food));
        CookingBlastDefaults.foods().forEach(food -> definitions.put(food.id(), food));
        for (String id : REQUESTED_FOODS) normalizeFood(root, id, definitions.get(id));
    }

    public static Set<String> cookingIngredientIds() {
        return REQUESTED_INGREDIENTS;
    }

    public static Set<String> customFoodIds() {
        return REQUESTED_FOODS;
    }

    public static Set<String> shopIds() {
        LinkedHashSet<String> ids = new LinkedHashSet<>(List.of(
                "general_cooking_shop", "bakers_pantry", "fishermans_kitchen", "butchers_counter",
                "farm_kitchen_supplies"));
        ids.addAll(FoodVendorDefaults.shopIds());
        return Set.copyOf(ids);
    }

    private static void normalizeIngredient(ConfigurationSection root, String id) {
        String base = "custom-items.items." + id;
        ConfigurationSection item = section(root, base);
        if (item.getString("name", "").isBlank()) item.set("name", "&f" + pretty(id));
        if (item.getString("material", "").isBlank()) item.set("material", "PAPER");
        item.set("type", "COOKING_INGREDIENT");
        item.set("mmo.type", "MATERIAL");
        item.set("category", "cooking_ingredients");
        item.set("rarity", UNCOMMON_INGREDIENTS.contains(id) ? "UNCOMMON" : "COMMON");
        item.set("lore", List.of("&7A prepared cooking ingredient."));
        applyOrdinarySafety(item);
        item.set("consumable.enabled", false);
        item.set("max-stack-size", 64);
        item.set("tradeable", true);
    }

    private static void normalizeFood(ConfigurationSection root, String id, CookingFoodDefaults40.Food definition) {
        String base = "custom-items.items." + id;
        ConfigurationSection item = section(root, base);
        if (definition != null) {
            item.set("name", definition.name());
            item.set("material", definition.material());
            item.set("rarity", safeFoodRarity(definition.rarity(), definition.heal(), definition.mana()));
            item.set("food.effect-type", definition.heal() > 0 && definition.mana() > 0
                    ? "MIXED" : definition.mana() > 0 ? "MANA" : "HEAL");
            item.set("food.heal", definition.heal());
            item.set("food.mana", definition.mana());
        } else {
            if (item.getString("name", "").isBlank()) item.set("name", "&a" + pretty(id));
            if (item.getString("material", "").isBlank()) item.set("material", fallbackFoodMaterial(id));
            int heal = item.getInt("food.heal", 0);
            int mana = item.getInt("food.mana", 0);
            item.set("rarity", safeFoodRarity(item.getString("rarity", ""), heal, mana));
            item.set("food.effect-type", heal > 0 && mana > 0 ? "MIXED" : mana > 0 ? "MANA" : "HEAL");
        }
        item.set("type", "CUSTOM_FOOD");
        item.set("mmo.type", "FOOD");
        item.set("category", "cooking");
        if (item.getStringList("lore").isEmpty()) item.set("lore", List.of("&7Prepared through the cooking system."));
        item.set("food-effect-id", id);
        item.set("consumable.enabled", true);
        int hunger = Math.max(0, item.getInt("food.hungerRestore", 4));
        double saturation = Math.max(0.0D, item.getDouble("food.saturationRestore", 2.4D));
        item.set("food.hungerRestore", hunger);
        item.set("food.saturationRestore", saturation);
        item.set("consumable.food", hunger);
        item.set("consumable.saturation", saturation);
        item.set("consumable.heal", Math.max(0, item.getInt("food.heal", 0)));
        item.set("consumable.mana", Math.max(0, item.getInt("food.mana", 0)));
        applyOrdinarySafety(item);
        item.set("max-stack-size", 64);
        item.set("tradeable", true);
    }

    private static void applyOrdinarySafety(ConfigurationSection item) {
        item.set("special-item", false);
        item.set("developer-notebook", false);
        item.set("random-stats-allowed", false);
        item.set("random-rarity-allowed", false);
        item.set("random-rarity", false);
        item.set("fixed-rarity", true);
        item.set("mmo.random-roll", false);
        item.set("mmo.stats", null);
        item.set("reforge-allowed", false);
        item.set("enchant-allowed", false);
        item.set("ability.type", "none");
        item.set("ability.mana-cost", 0);
    }

    private static String safeFoodRarity(String configured, int heal, int mana) {
        String rarity = configured == null ? "" : configured.trim().toUpperCase(Locale.ROOT);
        int strength = Math.max(0, heal) + Math.max(0, mana);
        String floor = strength >= 175 ? "EPIC" : strength >= 100 ? "RARE" : strength >= 50 ? "UNCOMMON" : "COMMON";
        List<String> order = List.of("COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC");
        return order.indexOf(rarity) >= order.indexOf(floor) ? rarity : floor;
    }

    private static String fallbackFoodMaterial(String id) {
        if (id.contains("drink") || id.contains("tea") || id.contains("chocolate")) return "HONEY_BOTTLE";
        if (id.contains("soup") || id.contains("stew") || id.contains("feast") || id.contains("banquet")) return "RABBIT_STEW";
        if (id.contains("cookie") || id.contains("smore") || id.contains("snack")) return "COOKIE";
        if (id.contains("cake")) return "CAKE";
        if (id.contains("pie")) return "PUMPKIN_PIE";
        if (id.contains("fish") || id.contains("cod")) return "COOKED_COD";
        if (id.contains("salmon") || id.contains("sushi")) return "COOKED_SALMON";
        return "BREAD";
    }

    private static ConfigurationSection section(ConfigurationSection root, String path) {
        ConfigurationSection existing = root.getConfigurationSection(path);
        return existing == null ? root.createSection(path) : existing;
    }

    private static Set<String> orderedWords(String value) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (String word : value.strip().split("\\s+")) if (!word.isBlank()) ids.add(word.toLowerCase(Locale.ROOT));
        return Set.copyOf(ids);
    }

    private static String pretty(String id) {
        StringBuilder result = new StringBuilder();
        for (String part : id.split("_")) {
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.toString();
    }
}
