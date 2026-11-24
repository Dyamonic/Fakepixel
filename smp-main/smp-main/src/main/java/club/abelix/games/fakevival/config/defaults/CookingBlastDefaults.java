package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Java-only culinary recipes for the Cooking Engine's blast furnace station. */
public final class CookingBlastDefaults {
    private CookingBlastDefaults() {
    }

    private static final List<CookingFoodDefaults40.Food> FOODS = List.of(
            food("seared_steak_plate", "&fSeared Steak Plate", "COOKED_BEEF", "COMMON", 55, 0,
                    "m:BEEF*2,c:salt*1,c:pepper*1"),
            food("charred_pork_ribs", "&fCharred Pork Ribs", "COOKED_PORKCHOP", "COMMON", 60, 0,
                    "m:PORKCHOP*2,c:spice_mix*1"),
            food("crispy_chicken_bites", "&fCrispy Chicken Bites", "COOKED_CHICKEN", "COMMON", 55, 0,
                    "m:CHICKEN*2,c:flour*1,c:salt*1"),
            food("fire_roasted_mutton", "&aFire-Roasted Mutton", "COOKED_MUTTON", "UNCOMMON", 75, 0,
                    "m:MUTTON*2,c:hot_spice_mix*1"),
            food("volcanic_rabbit_skewer", "&aVolcanic Rabbit Skewer", "COOKED_RABBIT", "UNCOMMON", 80, 0,
                    "m:RABBIT*2,c:pepper*1,m:STICK*1"),
            food("blast_baked_potato", "&aBlast-Baked Potato", "BAKED_POTATO", "UNCOMMON", 65, 0,
                    "m:POTATO*3,c:butter*1,c:salt*1"),
            food("molten_cheese_bread", "&aMolten Cheese Bread", "BREAD", "UNCOMMON", 75, 0,
                    "c:bread_dough*1,c:cheese*1"),
            food("lava_spiced_stew", "&9Lava-Spiced Stew", "RABBIT_STEW", "RARE", 105, 0,
                    "m:BOWL*1,c:hot_spice_mix*1,c:cooked_meat_slice*2"),
            food("iron_pan_breakfast", "&9Iron-Pan Breakfast", "RABBIT_STEW", "RARE", 110, 0,
                    "m:EGG*2,c:cooked_meat_slice*1,c:potato_slices*1"),
            food("forge_grilled_cod", "&aForge-Grilled Cod", "COOKED_COD", "UNCOMMON", 75, 0,
                    "m:COD*2,c:salt*1,c:pepper*1"),
            food("forge_grilled_salmon", "&9Forge-Grilled Salmon", "COOKED_SALMON", "RARE", 105, 0,
                    "m:SALMON*2,m:HONEY_BOTTLE*1,c:spice_mix*1"),
            food("crispy_kelp_chips", "&fCrispy Kelp Chips", "DRIED_KELP", "COMMON", 35, 0,
                    "m:KELP*8,c:salt*1"),
            food("blackstone_burger", "&9Blackstone Burger", "COOKED_BEEF", "RARE", 120, 0,
                    "m:BREAD*1,m:COOKED_BEEF*1,c:cheese*1"),
            food("miners_lava_lunch", "&9Miner's Lava Lunch", "BAKED_POTATO", "RARE", 135, 0,
                    "m:BAKED_POTATO*2,c:cooked_meat_slice*2,c:cheese*1"),
            food("scorched_mushroom_mix", "&aScorched Mushroom Mix", "MUSHROOM_STEW", "UNCOMMON", 80, 0,
                    "m:BROWN_MUSHROOM*2,m:RED_MUSHROOM*2,c:butter*1"),
            food("furnace_fried_fish", "&9Furnace-Fried Fish", "COOKED_COD", "RARE", 115, 0,
                    "c:fish_fillet*2,c:flour*1,c:salt*1"),
            food("blazing_salmon_roll", "&9Blazing Salmon Roll", "COOKED_SALMON", "RARE", 135, 10,
                    "c:salmon_fillet*2,c:hot_spice_mix*1,m:DRIED_KELP*1"),
            food("golden_forge_feast", "&5Golden Forge Feast", "GOLDEN_CARROT", "EPIC", 190, 25,
                    "m:GOLDEN_CARROT*1,c:cooked_meat_slice*3,c:golden_sauce*1"),
            food("nether_heat_platter", "&5Nether Heat Platter", "BLAZE_POWDER", "EPIC", 210, 35,
                    "m:BLAZE_POWDER*1,c:hot_spice_mix*1,c:cooked_meat_slice*3"),
            food("royal_forge_banquet", "&6Royal Forge Banquet", "GOLDEN_APPLE", "LEGENDARY", 300, 75,
                    "c:golden_forge_feast*1,c:forge_grilled_salmon*1,c:molten_cheese_bread*1")
    );

    public static void apply(ConfigurationSection root) {
        for (CookingFoodDefaults40.Food food : FOODS) {
            String base = "custom-items.items." + food.id();
            ConfigurationSection item = section(root, base);
            item.set("name", food.name());
            item.set("material", food.material());
            item.set("type", "CUSTOM_FOOD");
            item.set("mmo.type", "FOOD");
            item.set("category", "cooking");
            item.set("rarity", food.rarity());
            item.set("lore", loreFor(food.id()));
            item.set("food.effect-type", food.mana() > 0 ? "MIXED" : "HEAL");
            item.set("food.heal", food.heal());
            item.set("food.mana", food.mana());
            item.set("food-effect-id", food.id());
            item.set("consumable.enabled", true);
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
            item.set("max-stack-size", 64);

            String recipe = "cooking.recipes." + food.id();
            root.set(recipe + ".stations", food.stations());
            root.set(recipe + ".ingredients", food.ingredients());
            root.set(recipe + ".heal", food.heal());
            root.set(recipe + ".mana", food.mana());
        }
    }

    public static List<CookingFoodDefaults40.Food> foods() {
        return FOODS;
    }

    public static Set<String> ids() {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        FOODS.forEach(food -> ids.add(food.id()));
        return Set.copyOf(ids);
    }

    private static CookingFoodDefaults40.Food food(String id, String name, String material, String rarity,
                                                    int heal, int mana, String ingredients) {
        Map<String, Integer> parsed = new LinkedHashMap<>();
        for (String entry : ingredients.split(",")) {
            String[] parts = entry.split("\\*");
            parsed.put(parts[0], Integer.parseInt(parts[1]));
        }
        return new CookingFoodDefaults40.Food(id, name, material, rarity, List.of("BLAST_FURNACE"),
                heal, mana, Map.copyOf(parsed));
    }

    private static List<String> loreFor(String id) {
        return switch (id) {
            case "seared_steak_plate" -> List.of("&7Steak seared rapidly over forge heat.", "&7Salt and pepper lock in its flavor.");
            case "charred_pork_ribs" -> List.of("&7Spiced ribs with a smoky charred crust.", "&7A favorite meal among forge workers.");
            case "crispy_chicken_bites" -> List.of("&7Floured chicken crisped in intense heat.", "&7Small bites with a satisfying crunch.");
            case "fire_roasted_mutton" -> List.of("&7Mutton roasted with a fierce spice blend.", "&7Its warmth lingers after every bite.");
            case "volcanic_rabbit_skewer" -> List.of("&7Peppered rabbit roasted on a sturdy skewer.", "&7Cooked quickly beside molten stone.");
            case "blast_baked_potato" -> List.of("&7A fluffy potato with butter and sea salt.", "&7The forge leaves its skin perfectly crisp.");
            case "molten_cheese_bread" -> List.of("&7Fresh dough surrounding molten cheese.", "&7Best eaten while the center is warm.");
            case "lava_spiced_stew" -> List.of("&7A thick meat stew carrying volcanic heat.", "&7Only bold adventurers finish the bowl.");
            case "iron_pan_breakfast" -> List.of("&7Eggs, meat, and potato cooked in iron.", "&7A complete breakfast for busy miners.");
            case "forge_grilled_cod" -> List.of("&7Cod flash-grilled with salt and pepper.", "&7Crisp outside while remaining flaky within.");
            case "forge_grilled_salmon" -> List.of("&7Honey-glazed salmon cooked at forge heat.", "&7Sweet spice balances its rich flavor.");
            case "crispy_kelp_chips" -> List.of("&7Thin kelp leaves roasted until crisp.", "&7A light, salty snack from the forge.");
            case "blackstone_burger" -> List.of("&7A heavy cheeseburger toasted on blackstone.", "&7Built to satisfy a miner's appetite.");
            case "miners_lava_lunch" -> List.of("&7Potato, meat, and cheese in one hot meal.", "&7Packed for long shifts below the surface.");
            case "scorched_mushroom_mix" -> List.of("&7Two mushrooms browned in bubbling butter.", "&7Earthy flavor with a smoky finish.");
            case "furnace_fried_fish" -> List.of("&7Floured fish fried by concentrated heat.", "&7Golden, crisp, and lightly salted.");
            case "blazing_salmon_roll" -> List.of("&7Spicy salmon wrapped in dried sea kelp.", "&7A blazing twist on a fisher's classic.");
            case "golden_forge_feast" -> List.of("&7Golden vegetables crown a hearty meat feast.", "&7Finished with a rich golden sauce.");
            case "nether_heat_platter" -> List.of("&7A fiery platter infused with blaze powder.", "&7Its heat rivals the depths of the Nether.");
            case "royal_forge_banquet" -> List.of("&7Three masterworks united in one banquet.", "&7A royal reward for a legendary forge cook.");
            default -> List.of("&7A carefully prepared high-heat meal.", "&7Cooked in the SMP blast furnace kitchen.");
        };
    }

    private static ConfigurationSection section(ConfigurationSection root, String path) {
        ConfigurationSection existing = root.getConfigurationSection(path);
        return existing == null ? root.createSection(path) : existing;
    }
}
