package club.abelix.games.fakevival.config.defaults;

import club.abelix.games.fakevival.util.VendorLoreSanitizer;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashMap;

public final class NpcShopDefaults {
    private NpcShopDefaults() {}
    public static final int VILLAGE_SHOP_DEFAULT_VERSION = 3;
    private static int reusedCustomItems;
    private static int skippedOptionalItems;
    private static final Map<String, List<String>> skippedOptionalByShop = new LinkedHashMap<>();

    private static final List<String> IDS = List.of(("""
castle_royal_blacksmith castle_knight_trainer castle_royal_food_vendor castle_banker castle_guild_clerk castle_auction_master castle_royal_cosmetic_vendor
hayvillage_farmer_merchant hayvillage_baker hayvillage_barn_vendor hayvillage_compost_trader hayvillage_farming_collection_npc
purple_magic_merchant purple_potion_brewer purple_enchanter purple_rune_vendor purple_wizard
bay_fisherman bay_fishmonger bay_boat_merchant bay_seafood_chef bay_dock_worker
medieval_general_merchant medieval_blacksmith medieval_hunter_shop medieval_innkeeper medieval_town_guard
desert_trader desert_spice_merchant desert_explorer desert_relic_vendor desert_travel_npc
riverfern_herbalist riverfern_fisherman riverfern_alchemist riverfern_forager riverfern_river_cook
frosty_blacksmith frosty_warm_food_vendor frosty_miner frosty_hunter frosty_cosmetic_vendor
forest_lumberjack forest_foraging_merchant forest_hunter forest_campfire_cook forest_nature_quest_npc
palm_relic_trader palm_desert_priest palm_explorer_merchant palm_sun_charm_vendor palm_dungeon_gatekeeper
mansion_butler mansion_rich_merchant mansion_collector mansion_librarian mansion_secret_dealer
swamp_witch_doctor swamp_mushroom_vendor swamp_slime_hunter swamp_bog_fisherman swamp_cursed_relic_trader
cherry_blossom_priest cherry_petal_merchant cherry_silk_weaver cherry_temple_cook cherry_spirit_quest_npc
grain_farmer grain_mill_worker grain_baker grain_breakfast_vendor grain_farming_toolsmith
""").strip().split("\\s+"));

    private static final Set<String> UNCHANGED_CASTLE_SERVICES = Set.of(
            "castle_banker", "castle_guild_clerk", "castle_auction_master");
    private static final Set<String> SPECIAL_MENUS = Set.of("castle_banker", "castle_guild_clerk", "castle_auction_master",
            "hayvillage_farming_collection_npc", "purple_wizard", "medieval_town_guard", "desert_travel_npc",
            "forest_nature_quest_npc", "palm_dungeon_gatekeeper", "cherry_spirit_quest_npc");
    private static final List<String> NORMAL_VILLAGE_SHOP_IDS = IDS.stream()
            .filter(id -> !SPECIAL_MENUS.contains(id))
            .toList();

    public static List<String> ids() { return IDS; }
    public static List<String> villageIds() { return IDS; }
    public static List<String> normalVillageShopIds() { return NORMAL_VILLAGE_SHOP_IDS; }
    public static boolean isSpecialMenu(String id) { return id != null && SPECIAL_MENUS.contains(id.toLowerCase(Locale.ROOT)); }
    public static boolean isUnchangedCastleService(String id) {
        return id != null && UNCHANGED_CASTLE_SERVICES.contains(id.toLowerCase(Locale.ROOT));
    }

    public static void apply(ConfigurationSection root) {
        reusedCustomItems = 0;
        skippedOptionalItems = 0;
        skippedOptionalByShop.clear();
        ExistingItemLookup lookup = ExistingItemLookup.from(root);
        for (String id : IDS) {
            if (isSpecialMenu(id)) continue;
            String base = "shops." + id;
            // These IDs are versioned Java defaults: replace stale registry entries instead of merging them.
            root.set(base + ".items", null);
            root.set(base + ".name", "&6" + pretty(id));
            root.set(base + ".icon", icon(id));
            root.set(base + ".description", List.of("&7Browse this vendor's carefully selected wares."));
            root.set(base + ".source", "JAVA_DEFAULTS");
            root.set(base + ".default-version", VILLAGE_SHOP_DEFAULT_VERSION);
            root.set(base + ".buy-enabled", true);
            boolean sellEnabled = SELL_STOCK.containsKey(id) || isFishShop(id);
            root.set(base + ".sell-enabled", sellEnabled);
            int index = 0;
            for (String token : UNIQUE_STOCK.getOrDefault(id, List.of())) {
                String key = "buy_" + index++;
                if (token.startsWith("m:")) {
                    String material = token.substring(2);
                    if (!validVanilla(material)) { recordSkipped(id, material); continue; }
                    double price = vanillaPrice(material);
                    addVanilla(root, base, key, material, 1, price, 0D);
                } else {
                    String customId = token.startsWith("c:") ? token.substring(2) : token;
                    if (!lookup.safe(customId)) { recordSkipped(id, customId); continue; }
                    sanitizeExistingItem(root, customId, id);
                    reusedCustomItems++;
                    double price = customPrice(root, customId);
                    addCustom(root, base, key, customId, 1, price, 0D);
                }
                root.set(base + ".items." + key + ".allow-sell", false);
            }
            int sellIndex = 0;
            for (String token : SELL_STOCK.getOrDefault(id, List.of())) {
                String key = "sell_" + sellIndex++;
                if (token.startsWith("m:")) {
                    String material = token.substring(2);
                    if (!validVanilla(material)) { recordSkipped(id, material); continue; }
                    double buy = vanillaPrice(material);
                    addVanilla(root, base, key, material, 1, buy, Math.max(1D, buy * .45D));
                } else {
                    String customId = token.startsWith("c:") ? token.substring(2) : token;
                    if (!lookup.safe(customId)) { recordSkipped(id, customId); continue; }
                    sanitizeExistingItem(root, customId, id);
                    reusedCustomItems++;
                    double buy = customPrice(root, customId);
                    addCustom(root, base, key, customId, 1, buy, Math.max(1D, buy * .45D));
                }
                root.set(base + ".items." + key + ".allow-buy", false);
                root.set(base + ".items." + key + ".allow-sell", true);
            }
            if (isFishShop(id) && root.contains("custom-items.items.brook_minnow")) {
                addCustom(root, base, "custom_fish", "brook_minnow", 1, 45, 20);
                root.set(base + ".items.custom_fish.accept-custom-fish", true);
            }
            ensureMinimumStock(root, base, id);
        }
    }

    public static int reusedCustomItemCount() { return reusedCustomItems; }
    public static int skippedOptionalItemCount() { return skippedOptionalItems; }
    public static int generatedPlaceholderCount() { return 0; }
    public static Map<String, List<String>> skippedOptionalItemsByShop() { return Map.copyOf(skippedOptionalByShop); }

    private static void recordSkipped(String shopId, String itemId) {
        skippedOptionalItems++;
        skippedOptionalByShop.computeIfAbsent(shopId, ignored -> new ArrayList<>()).add(itemId);
    }

    public static void logRegistrations(org.bukkit.plugin.java.JavaPlugin plugin, ConfigurationSection root) {
        for (String id : IDS) {
            if (isSpecialMenu(id)) continue;
            ConfigurationSection items = root.getConfigurationSection("shops." + id + ".items");
            int entries = items == null ? 0 : items.getKeys(false).size();
            plugin.getLogger().info("[SMP Shops] Registered shop " + id + " entries=" + entries + " source=JAVA_DEFAULTS");
        }
        plugin.getLogger().info("[SMP Shops] Existing registry stock reused=" + reusedCustomItems
                + " optional-missing-skipped=" + skippedOptionalItems + " generated-placeholders=0");
        skippedOptionalByShop.forEach((shop, items) -> plugin.getLogger().warning(
                "[SMP Shops] Optional existing stock missing or blocked for " + shop + ": " + String.join(", ", items)
                        + "; skipped safely."));
    }

    private static final Map<String,List<String>> UNIQUE_STOCK = uniqueStock();
    private static final Map<String,List<String>> SELL_STOCK = sellStock();
    private static Map<String,List<String>> sellStock() {
        String data = """
castle_royal_blacksmith|m:IRON_INGOT,m:RAW_IRON,m:IRON_NUGGET,c:royal_scrap,c:broken_guard_hilt
castle_knight_trainer|c:goblin_ear,c:dungeon_fang,c:cracked_monster_core,c:rotten_monster_flesh
castle_royal_food_vendor|m:WHEAT,m:EGG,m:MILK_BUCKET,m:APPLE,m:SWEET_BERRIES
hayvillage_farmer_merchant|m:WHEAT,m:CARROT,m:POTATO,m:BEETROOT,m:PUMPKIN,m:MELON_SLICE
hayvillage_baker|m:WHEAT,m:SUGAR,m:EGG,m:MILK_BUCKET,m:COCOA_BEANS
hayvillage_barn_vendor|m:LEATHER,m:FEATHER,m:WHITE_WOOL,m:EGG,m:BEEF,m:CHICKEN
hayvillage_compost_trader|m:WHEAT_SEEDS,m:ROTTEN_FLESH,m:OAK_LEAVES,m:OAK_SAPLING,m:WHEAT
purple_magic_merchant|c:crystal_fragment,m:GLOWSTONE_DUST,m:AMETHYST_SHARD,m:ENDER_PEARL,m:PHANTOM_MEMBRANE
purple_potion_brewer|m:NETHER_WART,m:SPIDER_EYE,m:GHAST_TEAR,m:BLAZE_POWDER,m:REDSTONE,m:GLOWSTONE_DUST
purple_enchanter|m:LAPIS_LAZULI,m:BOOK,m:PAPER,m:INK_SAC,c:enchanted_lapis_shard
purple_rune_vendor|c:apprentice_rune,c:mana_thread,c:crystal_fragment
bay_fisherman|m:COD,m:SALMON,m:PUFFERFISH,m:TROPICAL_FISH
bay_fishmonger|m:COD,m:SALMON,m:PUFFERFISH,m:TROPICAL_FISH
bay_boat_merchant|m:STRING,m:OAK_PLANKS,m:IRON_NUGGET
bay_seafood_chef|m:COD,m:SALMON,m:KELP,m:BOWL
bay_dock_worker|m:KELP,m:OAK_PLANKS,m:STRING,m:IRON_NUGGET
medieval_general_merchant|m:COBBLESTONE,m:OAK_LOG,m:COAL,m:APPLE,m:LEATHER
medieval_blacksmith|m:IRON_INGOT,m:COAL,m:COPPER_INGOT,c:ancient_gear_fragment
medieval_hunter_shop|m:ROTTEN_FLESH,m:BONE,m:STRING,m:SPIDER_EYE,m:GUNPOWDER,c:dungeon_fang
medieval_innkeeper|m:MUTTON,m:BROWN_MUSHROOM,m:RED_MUSHROOM,m:APPLE,m:BOWL
desert_trader|m:CACTUS,m:SAND,m:RED_SAND,m:RABBIT_HIDE
desert_spice_merchant|m:SUGAR,m:WHEAT,m:COCOA_BEANS,m:BROWN_MUSHROOM,m:RABBIT
desert_explorer|m:BONE,m:STRING,m:PAPER,c:ancient_gear_fragment
desert_relic_vendor|c:ancient_gear_fragment,c:ancient_bone,m:GOLD_NUGGET,m:EMERALD
riverfern_herbalist|m:FERN,m:SHORT_GRASS,m:SWEET_BERRIES,m:LILY_PAD,m:BROWN_MUSHROOM
riverfern_fisherman|m:COD,m:SALMON,m:LILY_PAD,m:CLAY_BALL
riverfern_alchemist|m:GLOW_BERRIES,m:SPIDER_EYE,m:SUGAR,m:RED_MUSHROOM,c:mana_leaf
riverfern_forager|m:OAK_LOG,m:OAK_LEAVES,m:BROWN_MUSHROOM,m:RED_MUSHROOM,m:SWEET_BERRIES
riverfern_river_cook|m:COD,m:SALMON,m:BROWN_MUSHROOM,m:RED_MUSHROOM,m:SWEET_BERRIES,m:BOWL,m:KELP
frosty_blacksmith|m:IRON_INGOT,m:PACKED_ICE,m:BLUE_ICE,m:BONE,m:COAL
frosty_warm_food_vendor|m:POTATO,m:RABBIT,m:MILK_BUCKET,m:COCOA_BEANS,m:BOWL
frosty_miner|m:COAL,m:IRON_ORE,m:COPPER_ORE,m:EMERALD,c:ancient_ore_chunk
frosty_hunter|m:BONE,m:STRING,m:RABBIT_HIDE,m:LEATHER,m:PHANTOM_MEMBRANE
forest_lumberjack|m:OAK_LOG,m:SPRUCE_LOG,m:BIRCH_LOG,m:JUNGLE_LOG,m:ACACIA_LOG,m:DARK_OAK_LOG,m:MANGROVE_LOG,m:CHERRY_LOG,m:STICK
forest_foraging_merchant|m:OAK_LEAVES,m:BROWN_MUSHROOM,m:RED_MUSHROOM,m:SWEET_BERRIES,m:VINE,m:OAK_SAPLING
forest_hunter|m:BONE,m:LEATHER,m:FEATHER,m:STRING,c:dungeon_claw
forest_campfire_cook|m:PORKCHOP,m:CHICKEN,m:BROWN_MUSHROOM,m:RED_MUSHROOM,m:SWEET_BERRIES,m:COAL
palm_relic_trader|c:ancient_gear_fragment,m:GOLD_NUGGET,m:EMERALD,c:ancient_dungeon_essence
palm_desert_priest|c:sun_fragment,m:GLOWSTONE_DUST,m:GOLD_NUGGET
palm_explorer_merchant|m:STRING,m:PAPER,m:BONE,c:ancient_gear_fragment
palm_sun_charm_vendor|c:sun_fragment,m:GOLD_NUGGET,m:AMETHYST_SHARD
mansion_butler|m:PAPER,m:FEATHER,m:MILK_BUCKET,m:SUGAR
mansion_rich_merchant|m:GOLD_INGOT,m:EMERALD,m:DIAMOND,c:royal_crest_fragment
mansion_collector|c:sunken_coin,c:deep_mine_relic,c:ancient_tool_relic,m:SKELETON_SKULL
mansion_librarian|m:PAPER,m:LEATHER,m:INK_SAC,m:FEATHER,c:apprentice_rune
mansion_secret_dealer|c:shadow_cloth,c:dungeon_fang,c:void_dust,c:undead_ash
swamp_witch_doctor|m:SPIDER_EYE,m:ROTTEN_FLESH,m:BROWN_MUSHROOM,m:RED_MUSHROOM,m:SLIME_BALL,c:toxic_vial
swamp_mushroom_vendor|m:BROWN_MUSHROOM,m:RED_MUSHROOM,c:mushroom_slice,c:cooked_mushroom_mix
swamp_slime_hunter|m:SLIME_BALL,m:SLIME_BLOCK,m:MAGMA_CREAM,c:cracked_monster_core
swamp_bog_fisherman|m:COD,m:PUFFERFISH,m:LILY_PAD,m:CLAY_BALL,c:raw_river_fish
swamp_cursed_relic_trader|c:undead_ash,m:BONE,c:shadow_dust,c:void_dust
cherry_blossom_priest|c:sakura_petals,m:GLOWSTONE_DUST,m:PAPER
cherry_petal_merchant|m:PINK_PETALS,m:PINK_DYE,m:CHERRY_LEAVES,m:CHERRY_SAPLING
cherry_silk_weaver|m:STRING,m:WHITE_WOOL,m:PINK_WOOL,c:moonlit_cloth
cherry_temple_cook|m:WHEAT,m:SUGAR,m:SWEET_BERRIES,m:EGG,m:MILK_BUCKET
grain_farmer|m:WHEAT,m:WHEAT_SEEDS,m:HAY_BLOCK
grain_mill_worker|m:WHEAT,m:WHEAT_SEEDS,m:HAY_BLOCK
grain_baker|c:flour,m:WHEAT,m:SUGAR,m:EGG
grain_breakfast_vendor|m:EGG,m:MILK_BUCKET,m:WHEAT,m:HONEY_BOTTLE,m:APPLE
grain_farming_toolsmith|m:IRON_INGOT,m:STICK,m:COBBLESTONE,m:COPPER_INGOT
""";
        Map<String,List<String>> result=new LinkedHashMap<>();
        for(String line:data.strip().split("\\R")){String[] p=line.split("\\|",2);result.put(p[0],List.of(p[1].split(",")));}
        return Map.copyOf(result);
    }
    private static Map<String,List<String>> uniqueStock() {
        String data = """
castle_royal_blacksmith|c:royal_guard_blade,c:royal_crossbow,c:royal_knight_chestplate,c:obsidian_shield,c:repairmasters_charm,c:royal_scrap,c:imperial_steel_fragment
castle_knight_trainer|c:training_bow,c:bandit_cutlass,c:minor_defense_charm,c:warriors_focus_charm,c:goblin_helmet,c:combat_ration
castle_royal_food_vendor|c:royal_bakers_feast,c:royal_cheese_roll,c:royal_feast_plate,c:garden_royal_feast,c:golden_honey_cake,c:mana_royal_feast,m:GOLDEN_CARROT
castle_royal_cosmetic_vendor|m:GOLDEN_HELMET,m:WHITE_BANNER,m:RED_CARPET,m:ARMOR_STAND,m:PAINTING,m:FIREWORK_ROCKET,m:ITEM_FRAME
hayvillage_farmer_merchant|c:farmers_hoe,c:farmers_gloves,c:golden_seed_pouch,c:blessed_seed,c:farmers_belt,m:WHEAT_SEEDS,m:BEETROOT_SEEDS
hayvillage_baker|c:flour,c:dough,c:bread_dough,c:cookie_dough,c:apple_hand_pie,c:pumpkin_spice_pie,c:honey_glazed_bread,c:chocolate_cookie
hayvillage_barn_vendor|m:HAY_BLOCK,m:LEAD,m:SADDLE,m:SHEARS,c:woven_wheat_cloth,c:egg_mix,c:raw_beef_slice
hayvillage_compost_trader|m:BONE_MEAL,m:COMPOSTER,c:fertile_essence,c:compost_charm,c:blessed_seed,c:enchanted_leaf_bundle,c:enchanted_oak_resin
purple_magic_merchant|c:crystal_fragment,c:amethyst_focus_crystal,c:lapis_focus_lens,c:mana_thread,c:arcane_wand,c:apprentice_staff,c:mana_ring
purple_potion_brewer|m:GLASS_BOTTLE,c:lesser_healing_potion,c:greater_healing_potion,c:lesser_mana_potion,c:greater_mana_potion,c:iron_skin_potion,c:swift_step_potion,c:healing_herbs
purple_enchanter|m:EXPERIENCE_BOTTLE,m:ENCHANTED_BOOK,m:BOOK,m:LAPIS_LAZULI,c:enchanters_charm,c:apprentice_rune,c:storm_rune
purple_rune_vendor|c:apprentice_rune,c:storm_rune,c:mana_thread,c:crystal_fragment,c:lapis_focus_lens,c:amethyst_focus_crystal,c:sun_fragment
bay_fisherman|c:beginner_rod,c:reinforced_rod,c:simple_bait,c:lucky_bait,c:fishermans_luck_charm,c:angler_charm
bay_fishmonger|c:fish_fillet,c:salmon_fillet,c:seaweed_bundle,c:sunken_coin,c:salmon_oil_vial,m:DRIED_KELP
bay_boat_merchant|m:OAK_BOAT,m:OAK_CHEST_BOAT,m:SPRUCE_BOAT,m:LEAD,m:CHEST,m:BARREL,c:kelp_woven_line
bay_seafood_chef|c:crispy_cod_bites,c:buttered_cod_plate,c:smoked_fish_skewer,c:spicy_fish_stew,c:seaweed_salmon_bowl,c:deep_sea_sushi,c:royal_seafood_stew
bay_dock_worker|m:LEAD,m:LANTERN,m:BARREL,m:CHEST,m:IRON_NUGGET,m:COMPASS,c:kelp_woven_line
medieval_general_merchant|m:TORCH,m:APPLE,m:STONE_PICKAXE,m:STONE_AXE,m:BUCKET,m:COMPASS,m:MAP,m:CHEST
medieval_blacksmith|m:IRON_PICKAXE,m:IRON_AXE,m:IRON_SWORD,m:ANVIL,c:repairmasters_charm,c:ancient_ore_chunk,c:dense_iron_fragment
medieval_hunter_shop|c:training_bow,c:bandit_shortbow,c:archers_focus_charm,c:hunters_fang_necklace,c:monster_bait,c:swift_step_potion,m:ARROW
medieval_innkeeper|c:combat_ration,c:mushroom_stew_custom,c:apple_hand_pie,c:mutton_spice_roast,c:warm_milk,c:campfire_toast,m:WHITE_BED
desert_trader|m:CACTUS,m:DEAD_BUSH,m:SAND,m:SANDSTONE,m:RED_SAND,m:GLASS,c:cactus_knife,c:sun_dust
desert_spice_merchant|c:salt,c:pepper,c:spice_mix,c:hot_spice_mix,c:spiced_flatbread,c:puffer_heat_stew,c:pumpkin_spice_pie
desert_explorer|m:COMPASS,m:MAP,m:TORCH,m:LEAD,m:BRUSH,c:ancient_key_fragment,c:dungeon_key_ancient,c:ancient_tool_relic
desert_relic_vendor|c:ancient_bone,c:ancient_dungeon_essence,c:ancient_gear_fragment,c:deep_mine_relic,c:ancient_guard_charm,c:royal_sun_necklace,c:sun_fragment
riverfern_herbalist|c:healing_herbs,c:healing_herb,c:mana_leaf,c:forest_berries,c:berry_jam,c:healing_herb_tea,m:FERN
riverfern_fisherman|c:river_rod,c:worm_bait,c:simple_bait,c:fishermans_pearl_charm,c:angler_charm,c:raw_river_fish
riverfern_alchemist|c:lesser_healing_potion,c:swift_step_potion,c:healing_herbs,c:mana_syrup,c:alchemists_charm,c:mushroom_slice,c:berry_jam
riverfern_forager|c:oak_cutter_axe,c:lumber_axe,c:enchanted_leaf_bundle,c:forest_berries,c:spruce_pinecone,c:lumberjacks_luck_charm
riverfern_river_cook|c:fishers_lunchbox,c:mushroom_stew_custom,c:mana_berry_tea,c:cooked_fish_fillet,c:healing_herb_tea,c:seaweed_salmon_bowl
frosty_blacksmith|c:crystalbound_pickaxe,c:frostguard_helmet,c:frostguard_boots,c:frost_wand,c:repairmasters_charm,c:ancient_light_shard,m:SHIELD
frosty_warm_food_vendor|c:hot_chocolate,c:warm_milk,c:mushroom_stew_custom,c:mutton_spice_roast,c:rabbit_herb_roast,c:miners_lunch,c:soulfire_tea
frosty_miner|c:crystalbound_pickaxe,m:DIAMOND_PICKAXE,c:miners_luck_charm,c:ancient_ore_chunk,c:ancient_light_shard,m:IRON_PICKAXE,m:LANTERN
frosty_hunter|c:training_bow,c:hunters_fang_necklace,c:monster_bait,c:frostguard_boots,c:archers_ration,m:ARROW,m:RABBIT_STEW
frosty_cosmetic_vendor|m:LIGHT_BLUE_BANNER,m:WHITE_CARPET,m:ARMOR_STAND,m:SOUL_LANTERN,m:SNOW_BLOCK,m:BLUE_ICE,m:FIREWORK_ROCKET
forest_lumberjack|c:oak_cutter_axe,c:birchwood_axe,c:jungle_cleaver_axe,c:lumber_axe,c:lumberjack_saw,c:lumberjacks_luck_charm
forest_foraging_merchant|c:enchanted_leaf_bundle,c:forest_berries,c:enchanted_oak_resin,c:spruce_pinecone,c:dark_oak_root,c:lumberjacks_luck_charm
forest_hunter|c:training_bow,c:bandit_shortbow,c:hunters_fang_necklace,c:monster_bait,c:archers_focus_charm,m:ARROW
forest_campfire_cook|c:campfire_toast,c:campfire_beef_bites,c:campfire_fish_skewer,c:campfire_smores,c:grand_campfire_feast,c:smoked_pork_slices,c:roasted_chicken_strips
palm_relic_trader|c:deep_mine_relic,c:ancient_tool_relic,c:ancient_gear_fragment,c:ancient_dungeon_essence,c:royal_sun_gem,c:furniture_token,c:sunken_coin
palm_desert_priest|c:greater_healing_potion,c:healing_herbs,c:ancient_guard_charm,c:totem_of_ancient_light,c:sun_fragment,m:GLOWSTONE
palm_explorer_merchant|m:MAP,m:TORCH,m:LEAD,m:BRUSH,c:ancient_key_fragment,c:dungeon_key_ancient,c:ancient_tool_relic
palm_sun_charm_vendor|c:ancient_guard_charm,c:moon_necklace,c:royal_sun_necklace,c:royal_sun_gem,c:farmers_luck_charm,c:fishermans_luck_charm
mansion_butler|c:royal_cheese_roll,c:apple_hand_pie,c:warm_milk,c:royal_bakers_feast,m:PAINTING,m:ITEM_FRAME,m:WHITE_CARPET
mansion_rich_merchant|m:GOLDEN_APPLE,c:moon_necklace,c:crystal_focus,c:royal_gold_plate,c:royal_sun_necklace,c:crystal_heart_necklace,m:PAINTING
mansion_collector|c:deep_mine_relic,c:ancient_tool_relic,c:sunken_coin,c:treasure_shell,c:event_cosmetic_token,c:furniture_token,m:SKELETON_SKULL
mansion_librarian|m:BOOK,m:BOOKSHELF,m:WRITABLE_BOOK,c:apprentice_rune,c:storm_rune,c:enchanters_charm,c:lapis_focus_lens
mansion_secret_dealer|c:shadow_dust,c:shadow_cloth,c:void_dust,c:void_wand,c:undead_ash,c:dungeon_fang,c:ancient_key_fragment
swamp_witch_doctor|c:toxic_vial,m:SPIDER_EYE,c:mushroom_slice,c:toxic_wand,c:healing_herbs,c:lesser_healing_potion,c:shadow_dust
swamp_mushroom_vendor|m:BROWN_MUSHROOM,m:RED_MUSHROOM,c:mushroom_slice,c:cooked_mushroom_mix,c:mushroom_stew_custom,c:mushroom_bake,c:spiced_mushroom
swamp_slime_hunter|m:SLIME_BALL,m:SLIME_BLOCK,m:MAGMA_CREAM,c:cracked_monster_core,c:dungeon_claw,c:monster_bait,m:STICKY_PISTON
swamp_bog_fisherman|c:river_rod,c:worm_bait,c:spicy_bait,c:fishermans_ring,c:raw_river_fish,c:angler_charm,m:LILY_PAD
swamp_cursed_relic_trader|c:undead_ash,c:shadow_dust,c:shadow_cloth,c:void_dust,m:GHAST_TEAR,c:ancient_guard_charm,c:monster_soul_shard
cherry_blossom_priest|c:sakura_petals,c:cherry_blossom_petal,c:healing_herbs,c:greater_healing_potion,c:ancient_guard_charm,m:SOUL_LANTERN,m:PAPER
cherry_petal_merchant|c:sakura_petals,c:cherry_blossom_petal,c:cherry_wood_inlay,m:PINK_DYE,m:CHERRY_SAPLING,m:PINK_PETALS,m:FLOWER_POT
cherry_silk_weaver|c:moonlit_cloth,c:nightfall_cloth,c:phantom_silk_membrane,c:imperial_cloth,m:STRING,m:WHITE_WOOL,m:PINK_BANNER
cherry_temple_cook|c:berry_pie,c:apple_cinnamon_pie,c:honey_glazed_bread,c:chocolate_cookie,c:mana_berry_tea,c:golden_honey_cake,c:garden_royal_feast
grain_farmer|c:farmers_hoe,c:golden_seed_pouch,c:blessed_seed,c:farmers_gloves,c:farmers_belt,m:WHEAT_SEEDS,m:HAY_BLOCK
grain_mill_worker|c:flour,c:dough,c:bread_dough,c:sweet_dough,c:enchanted_flour,c:woven_wheat_cloth,c:dense_hay_fiber
grain_baker|c:honey_glazed_bread,c:enchanted_bread_loaf,c:chocolate_cookie,c:apple_hand_pie,c:pumpkin_spice_pie,c:golden_honey_cake,c:royal_bakers_feast
grain_breakfast_vendor|c:fishermans_breakfast,c:garden_breakfast,c:hearty_breakfast_plate,c:warm_milk,c:sweet_cream_drink,c:honey_milk,c:miners_lunch
grain_farming_toolsmith|m:STONE_HOE,m:IRON_HOE,c:harvesting_hoe,c:beetroot_sickle,c:cactus_knife,c:farmers_scythe,c:harvest_hoe
""";
        Map<String,List<String>> result = new LinkedHashMap<>();
        for (String line : data.strip().split("\\R")) { String[] p=line.split("\\|",2); result.put(p[0],List.of(p[1].split(","))); }
        return Map.copyOf(result);
    }

    private static List<String> uniqueVanilla(String id) {
        List<String> pool = themedVanilla(id);
        int first = Math.floorMod(id.hashCode(), pool.size());
        int second = (first + 1 + Math.floorMod(id.hashCode() / 7, pool.size() - 1)) % pool.size();
        return List.of(pool.get(first), pool.get(second));
    }

    private static void sanitizeExistingItem(ConfigurationSection root, String id, String shopId) {
        String base = "custom-items.items." + id;
        List<String> original = root.getStringList(base + ".lore");
        List<String> cleaned = VendorLoreSanitizer.sanitizeVendorLore(original);
        if (cleaned.size() != original.size()) {
            String type = root.getString(base + ".type", vendorType(id, shopId));
            root.set(base + ".lore", cleaned.stream().anyMatch(line -> line != null && !line.isBlank())
                    ? cleaned : vendorLore(id, type));
        }
    }

    private static boolean validVanilla(String materialName) {
        Material material = Material.matchMaterial(materialName);
        return material != null && material != Material.AIR && material != Material.CAVE_AIR
                && material != Material.VOID_AIR && !material.isLegacy();
    }

    private static void ensureMinimumStock(ConfigurationSection root, String base, String shopId) {
        ConfigurationSection items = root.getConfigurationSection(base + ".items");
        int count = items == null ? 0 : items.getKeys(false).size();
        if (count >= 5) return;
        Set<String> present = new HashSet<>();
        if (items != null) {
            for (String key : items.getKeys(false)) {
                String material = items.getString(key + ".material");
                if (material != null) present.add(material.toUpperCase(Locale.ROOT));
            }
        }
        for (String material : themedVanilla(shopId)) {
            if (count >= 5) break;
            if (!validVanilla(material) || !present.add(material)) continue;
            String key = "fallback_" + count;
            addVanilla(root, base, key, material, 1, vanillaPrice(material), 0D);
            root.set(base + ".items." + key + ".allow-sell", false);
            count++;
        }
    }

    private record ExistingItemLookup(Set<String> safeIds) {
        static ExistingItemLookup from(ConfigurationSection root) {
            ConfigurationSection items = root.getConfigurationSection("custom-items.items");
            Set<String> safe = new HashSet<>();
            if (items == null) return new ExistingItemLookup(Set.of());
            for (String id : items.getKeys(false)) {
                String path = "custom-items.items." + id + ".";
                String normalizedId = id.toLowerCase(Locale.ROOT);
                String type = root.getString(path + "type", root.getString(path + "mmo.type", ""));
                String rarity = root.getString(path + "rarity", "");
                boolean blocked = normalizedId.equals("developer_notebook")
                        || "SPECIAL".equalsIgnoreCase(type) || "SPECIAL".equalsIgnoreCase(rarity)
                        || root.getBoolean(path + "special-item") || root.getBoolean(path + "special_item")
                        || root.getBoolean(path + "developer-notebook") || root.getBoolean(path + "developer_notebook")
                        || root.getBoolean(path + "admin-item") || root.getBoolean(path + "admin_item")
                        || root.getBoolean(path + "preview-item") || root.getBoolean(path + "preview_item")
                        || normalizedId.contains("admin") || normalizedId.contains("test_item") || normalizedId.contains("preview");
                if (!blocked) safe.add(normalizedId);
            }
            return new ExistingItemLookup(Set.copyOf(safe));
        }

        boolean safe(String id) {
            return id != null && safeIds.contains(id.toLowerCase(Locale.ROOT));
        }
    }

    public static List<String> vendorLore(String id, String type) {
        if (id == null || id.isBlank()) return List.of("&7A useful village trade item.", "&7Used in crafting and upgrades.");
        String key = id.toLowerCase(Locale.ROOT);
        return switch (key) {
            case "bandit_badge" -> List.of("&7Proof taken from a defeated bandit.", "&7Used for knight training trades.");
            case "combat_xp_scroll" -> List.of("&7A training scroll used by castle knights.", "&7Grants combat progression when consumed.");
            case "practice_dummy" -> List.of("&7A sturdy dummy for testing your strikes.", "&7Useful for early combat training.");
            case "sparring_bandage" -> List.of("&7A simple bandage used after training.", "&7Restores a small amount of health.");
            case "duel_token" -> List.of("&7A token used to enter castle sparring matches.", "&7Trade it with the Knight Trainer.");
            case "training_manual" -> List.of("&7A handbook of basic combat drills.", "&7Study it to improve your training.");
            default -> categoryVendorLore(key, type);
        };
    }

    private static List<String> categoryVendorLore(String id, String type) {
        String normalizedType = type == null ? "MATERIAL" : type.toUpperCase(Locale.ROOT);
        if (id.matches(".*(training|combat|duel|sparring|bandit|goblin|monster|fang|shield|hunter|battle).*"))
            return List.of("&7Used by seasoned fighters during training.", "&7Can be traded for combat supplies.");
        if (normalizedType.equals("FOOD") || id.matches(".*(bread|meal|stew|soup|tea|juice|cake|pie|toast).*"))
            return List.of("&7A carefully prepared village meal.", "&7Restores hunger and grants a small bonus.");
        if (id.matches(".*(relic|artifact|ancient|scarab|museum|ruin|trophy|coin).*"))
            return List.of("&7An old relic recovered from distant ruins.", "&7Used in artifact trades and upgrades.");
        if (normalizedType.equals("COSMETIC") || id.matches(".*(furniture|chair|table|carpet|statue|vase|banner|lamp|shrine|painting|sofa).*"))
            return List.of("&7A decorative item made for collectors.", "&7Cosmetic only.");
        if (id.matches(".*(fish|fishing|bait|rod|hook|tackle|dock|sea|salmon|cod).*"))
            return List.of("&7Used by fishers and dock workers.", "&7Helpful for fishing progression.");
        if (id.matches(".*(farm|crop|seed|grain|wheat|harvest|compost|fertilizer|hoe).*"))
            return List.of("&7Used by farmers for crop progression.", "&7Helpful for farming trades.");
        if (normalizedType.equals("POTION") || id.matches(".*(magic|mana|arcane|rune|spell|potion|brew|tonic).*"))
            return List.of("&7Infused with carefully prepared magical essence.", "&7Used for spellcraft and alchemy trades.");
        if (normalizedType.equals("TOOL"))
            return List.of("&7A practical tool made for village adventurers.", "&7Useful while gathering and exploring.");
        if (normalizedType.equals("ACCESSORY"))
            return List.of("&7A finely crafted adventurer's accessory.", "&7Useful for progression and specialized trades.");
        return List.of("&7A useful trade material gathered by adventurers.", "&7Used in village crafting and upgrades.");
    }

    private static String vendorType(String id, String shopId) {
        if (shopId.contains("cosmetic")) return "COSMETIC";
        if (id.matches(".*(food|bread|pie|cake|stew|soup|meal|juice|tea|cocoa|roll|toast|pancake|pretzel|baguette|salad|dates|biscuit|skewer|mushroom).*")) return "FOOD";
        if (id.matches(".*(potion|brew|tonic|elixir|cure|flask).*")) return "POTION";
        if (id.matches(".*(chair|table|carpet|statue|vase|shelf|desk|bed|crate|bin|lamp|shrine|display_case|millstone|scarecrow|anchor|throne|banner|curtain|firework|hut|painting|sofa).*")) return "COSMETIC";
        if (id.matches(".*(sword|axe|pickaxe|hoe|knife|sickle|wand|rod|bow|shield|boots|gloves|pick|blade|hammer).*")) return "TOOL";
        if (id.matches(".*(charm|ring|necklace|band|cape|cloak|crown|hat|wings|aura|garland).*")) return "ACCESSORY";
        return "MATERIAL";
    }
    private static void addVanilla(ConfigurationSection root, String base, String key, String material,
                                   int amount, double buy, double sell) {
        String path = base + ".items." + key;
        root.set(path + ".material", material); root.set(path + ".amount", amount);
        root.set(path + ".price", buy); root.set(path + ".sell-price", sell); root.set(path + ".allow-sell", true);
    }

    private static void addCustom(ConfigurationSection root, String base, String key, String customId,
                                  int amount, double buy, double sell) {
        String path = base + ".items." + key;
        root.set(path + ".custom-item", customId); root.set(path + ".amount", amount);
        root.set(path + ".price", buy); root.set(path + ".sell-price", sell); root.set(path + ".allow-sell", true);
    }

    private static boolean isFishShop(String id) { return id.contains("fisherman") || id.contains("fishmonger"); }
    private static boolean sellsResources(String id) { return id.contains("blacksmith") || id.contains("farmer") || id.contains("barn")
            || id.contains("hunter") || id.contains("trader") || id.contains("forager") || id.contains("miner")
            || id.contains("lumberjack") || id.contains("mushroom") || id.contains("petal"); }
    private static List<String> themedVanilla(String id) {
        if (isFishShop(id)) return List.of("FISHING_ROD", "STRING", "COD", "SALMON", "KELP", "COOKED_COD");
        if (id.contains("blacksmith") || id.contains("toolsmith")) return List.of("IRON_INGOT", "GOLD_INGOT", "DIAMOND", "NETHERITE_SCRAP", "ANVIL", "SHIELD");
        if (id.contains("farmer") || id.contains("grain")) return List.of("WHEAT_SEEDS", "WHEAT", "CARROT", "POTATO", "BEETROOT_SEEDS", "HAY_BLOCK");
        if (id.contains("baker") || id.contains("food") || id.contains("cook") || id.contains("innkeeper")) return List.of("BREAD", "COOKIE", "CAKE", "COOKED_BEEF", "COOKED_CHICKEN", "GOLDEN_CARROT");
        if (id.contains("magic") || id.contains("wizard") || id.contains("potion") || id.contains("alchemist") || id.contains("witch") || id.contains("rune") || id.contains("enchant")) return List.of("GLASS_BOTTLE", "NETHER_WART", "BLAZE_POWDER", "GLOWSTONE_DUST", "REDSTONE", "LAPIS_LAZULI");
        if (id.contains("hunter") || id.contains("knight")) return List.of("BOW", "ARROW", "SHIELD", "BONE", "STRING", "SPIDER_EYE");
        if (id.contains("forest") || id.contains("lumber") || id.contains("forag")) return List.of("OAK_LOG", "SPRUCE_LOG", "OAK_SAPLING", "VINE", "MOSS_BLOCK", "BONE_MEAL");
        if (id.contains("desert") || id.contains("palm") || id.contains("relic") || id.contains("explorer")) return List.of("SAND", "SANDSTONE", "RED_SAND", "GLASS", "CACTUS", "BRUSH");
        if (id.contains("swamp") || id.contains("mushroom") || id.contains("slime")) return List.of("BROWN_MUSHROOM", "RED_MUSHROOM", "MUSHROOM_STEW", "SLIME_BALL", "MAGMA_CREAM", "SPIDER_EYE");
        if (id.contains("cherry") || id.contains("silk")) return List.of("CHERRY_LOG", "CHERRY_SAPLING", "PINK_PETALS", "PINK_DYE", "WHITE_WOOL", "STRING");
        if (id.contains("frost")) return List.of("ICE", "SNOW_BLOCK", "PACKED_ICE", "COOKED_MUTTON", "RABBIT_STEW", "RABBIT_FOOT");
        if (id.contains("librarian")) return List.of("BOOK", "PAPER", "BOOKSHELF", "WRITABLE_BOOK", "INK_SAC", "LAPIS_LAZULI");
        return List.of("TORCH", "CHEST", "CRAFTING_TABLE", "FURNACE", "LADDER", "PAPER");
    }
    private static List<String> customCandidates(String id) {
        List<String> unique = UNIQUE_STOCK.get(id);
        if (unique != null) return unique;
        if (isFishShop(id)) return List.of("basic_fishing_rod_plus", "salmon_rod", "fish_fillet", "salmon_fillet", "common_bait");
        if (id.contains("blacksmith")) return List.of("iron_block_plate", "compressed_iron_plate", "heavy_iron_plate", "netherite_rivet", "raw_iron_chunk");
        if (id.contains("farmer") || id.contains("toolsmith")) return List.of("farmers_hoe", "enchanted_wheat", "wheat_grain_core", "carrot_cultivator", "potato_cultivator");
        if (id.contains("baker") || id.contains("food") || id.contains("cook")) return List.of("flour", "dough", "enchanted_bread", "honey_sticky_bread", "egg_yolk_breakfast");
        if (id.contains("magic") || id.contains("wizard") || id.contains("potion") || id.contains("alchemist")) return List.of("mana_syrup", "healing_herbs", "lapis_spell_dust", "amethyst_pulse_core", "mana_charm");
        if (id.contains("hunter") || id.contains("knight")) return List.of("training_sword", "undead_sword", "bone_blade", "defense_charm", "health_charm");
        if (id.contains("forest") || id.contains("forag")) return List.of("leafy_fiber", "vine_rope_bundle", "mossy_thread", "flower_essence", "woodcutters_axe");
        if (id.contains("desert") || id.contains("palm") || id.contains("relic")) return List.of("sandstone_core", "red_sand_heat_core", "cactus_spike_core", "ancient_sniffer_scale", "sandstone_sun_core");
        if (id.contains("swamp") || id.contains("mushroom") || id.contains("slime")) return List.of("mushroom_spore_mix", "toxic_vial", "slime_stretch_gel", "slime_elastic_core", "undead_ash");
        if (id.contains("cherry")) return List.of("cherry_silk_petal", "flower_essence", "dye_magic_dust", "honey_sticky_bread", "cocoa_dark_cookie");
        if (id.contains("frost")) return List.of("ice_crystal", "frosted_scale", "snowpacked_cloth", "ice_crystal_wand", "snowpacked_boots");
        if (id.contains("librarian")) return List.of("book_rune_page", "paper_spell_sheet", "recipe_book_upgrade");
        return List.of("portable_workbench", "portable_furnace", "paper_spell_sheet");
    }
    private static double vanillaPrice(String material) {
        return switch (material) { case "DIAMOND" -> 150; case "NETHERITE_SCRAP" -> 500; case "ANVIL" -> 145;
            case "SHIELD", "BOW", "FISHING_ROD" -> 125; case "CAKE", "GOLDEN_CARROT" -> 90; default -> 25; };
    }
    private static double customPrice(ConfigurationSection root, String id) {
        String type = root.getString("custom-items.items." + id + ".type", "MATERIAL").toUpperCase(Locale.ROOT);
        if (Set.of("WEAPON", "SWORD", "BOW", "ARMOR", "TOOL", "PICKAXE", "AXE", "FISHING_ROD", "WAND", "STAFF").contains(type)) return 3500;
        if (Set.of("RING", "NECKLACE", "CHARM", "ACCESSORY", "TOTEM").contains(type)) return 15000;
        if (Set.of("FOOD", "CUSTOM_FOOD", "POTION").contains(type)) return 500;
        return id.startsWith("enchanted_") || id.startsWith("compressed_") ? 1250 : 250;
    }
    private static String primaryMaterial(String id) {
        if (isFishShop(id)) return "COD";
        if (id.contains("blacksmith") || id.contains("toolsmith") || id.contains("miner")) return "IRON_INGOT";
        if (id.contains("farmer") || id.contains("grain") || id.contains("baker") || id.contains("barn")) return "WHEAT";
        if (id.contains("magic") || id.contains("wizard") || id.contains("enchant") || id.contains("rune") || id.contains("alchemist") || id.contains("witch")) return "AMETHYST_SHARD";
        if (id.contains("forest") || id.contains("lumber") || id.contains("forag")) return "OAK_LOG";
        if (id.contains("swamp") || id.contains("mushroom")) return "BROWN_MUSHROOM";
        if (id.contains("cherry")) return "CHERRY_LOG";
        if (id.contains("desert") || id.contains("palm")) return "SANDSTONE";
        return "IRON_INGOT";
    }
    private static String foodMaterial(String id) { return isFishShop(id) ? "COOKED_COD" : id.contains("desert") ? "COOKED_RABBIT" : "BREAD"; }
    private static String utilityMaterial(String id) { return isFishShop(id) ? "FISHING_ROD" : id.contains("magic") || id.contains("wizard") ? "GLASS_BOTTLE" : "TORCH"; }
    private static String icon(String id) { return isFishShop(id) ? "FISHING_ROD" : primaryMaterial(id); }
    private static String pretty(String id) {
        StringBuilder out = new StringBuilder();
        for (String part : id.split("_")) out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase(Locale.ROOT)).append(' ');
        return out.toString().trim();
    }
}
