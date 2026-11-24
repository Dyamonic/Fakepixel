package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RecipeDefaults {
    private RecipeDefaults() {
    }

    private static final String ENCHANTED_MATERIALS = """
            enchanted_coal|Enchanted Coal|COAL|mining|coal|COAL|2|UNCOMMON
            enchanted_raw_copper|Enchanted Raw Copper|RAW_COPPER|mining|copper|RAW_COPPER|2|UNCOMMON
            enchanted_copper_ingot|Enchanted Copper Ingot|COPPER_INGOT|mining|copper|COPPER_INGOT|3|UNCOMMON
            enchanted_raw_iron|Enchanted Raw Iron|RAW_IRON|mining|iron|RAW_IRON|2|UNCOMMON
            enchanted_iron_ingot|Enchanted Iron Ingot|IRON_INGOT|mining|iron|IRON_INGOT|3|UNCOMMON
            enchanted_raw_gold|Enchanted Raw Gold|RAW_GOLD|mining|gold|RAW_GOLD|2|UNCOMMON
            enchanted_gold_ingot|Enchanted Gold Ingot|GOLD_INGOT|mining|gold|GOLD_INGOT|3|UNCOMMON
            enchanted_lapis_lazuli|Enchanted Lapis Lazuli|LAPIS_LAZULI|mining|lapis|LAPIS_LAZULI|2|UNCOMMON
            enchanted_redstone|Enchanted Redstone|REDSTONE|mining|redstone|REDSTONE|2|UNCOMMON
            enchanted_emerald|Enchanted Emerald|EMERALD|mining|emerald|EMERALD|3|UNCOMMON
            enchanted_diamond|Enchanted Diamond|DIAMOND|mining|diamond|DIAMOND|3|UNCOMMON
            enchanted_quartz|Enchanted Quartz|QUARTZ|mining|quartz|QUARTZ|2|UNCOMMON
            enchanted_netherite_scrap|Enchanted Netherite Scrap|NETHERITE_SCRAP|mining|netherite|NETHERITE_SCRAP|5|RARE
            enchanted_ancient_debris|Enchanted Ancient Debris|ANCIENT_DEBRIS|mining|ancient_debris|ANCIENT_DEBRIS|5|RARE
            enchanted_amethyst_shard|Enchanted Amethyst Shard|AMETHYST_SHARD|mining|amethyst|AMETHYST_SHARD|2|UNCOMMON
            enchanted_echo_shard|Enchanted Echo Shard|ECHO_SHARD|mining|echo_shard|ECHO_SHARD|5|RARE
            enchanted_coal_block|Enchanted Coal Block|COAL_BLOCK|mining|coal|enchanted_coal|5|RARE
            enchanted_copper_block|Enchanted Copper Block|COPPER_BLOCK|mining|copper|enchanted_copper_ingot|5|RARE
            enchanted_iron_block|Enchanted Iron Block|IRON_BLOCK|mining|iron|enchanted_iron_ingot|5|RARE
            enchanted_gold_block|Enchanted Gold Block|GOLD_BLOCK|mining|gold|enchanted_gold_ingot|5|RARE
            enchanted_lapis_block|Enchanted Lapis Block|LAPIS_BLOCK|mining|lapis|enchanted_lapis_lazuli|5|RARE
            enchanted_redstone_block|Enchanted Redstone Block|REDSTONE_BLOCK|mining|redstone|enchanted_redstone|5|RARE
            enchanted_emerald_block|Enchanted Emerald Block|EMERALD_BLOCK|mining|emerald|enchanted_emerald|5|RARE
            enchanted_diamond_block|Enchanted Diamond Block|DIAMOND_BLOCK|mining|diamond|enchanted_diamond|5|RARE
            enchanted_quartz_block|Enchanted Quartz Block|QUARTZ_BLOCK|mining|quartz|enchanted_quartz|5|RARE
            enchanted_netherite_block|Enchanted Netherite Block|NETHERITE_BLOCK|mining|netherite|enchanted_netherite_scrap|7|RARE
            enchanted_amethyst_block|Enchanted Amethyst Block|AMETHYST_BLOCK|mining|amethyst|enchanted_amethyst_shard|5|RARE
            enchanted_cobblestone|Enchanted Cobblestone|COBBLESTONE|mining|cobblestone|COBBLESTONE|2|UNCOMMON
            enchanted_stone|Enchanted Stone|STONE|mining|stone|STONE|2|UNCOMMON
            enchanted_smooth_stone|Enchanted Smooth Stone|SMOOTH_STONE|mining|smooth_stone|SMOOTH_STONE|2|UNCOMMON
            enchanted_deepslate|Enchanted Deepslate|DEEPSLATE|mining|deepslate|DEEPSLATE|2|UNCOMMON
            enchanted_cobbled_deepslate|Enchanted Cobbled Deepslate|COBBLED_DEEPSLATE|mining|cobbled_deepslate|COBBLED_DEEPSLATE|2|UNCOMMON
            enchanted_blackstone|Enchanted Blackstone|BLACKSTONE|mining|blackstone|BLACKSTONE|2|UNCOMMON
            enchanted_basalt|Enchanted Basalt|BASALT|mining|basalt|BASALT|2|UNCOMMON
            enchanted_tuff|Enchanted Tuff|TUFF|mining|tuff|TUFF|2|UNCOMMON
            enchanted_calcite|Enchanted Calcite|CALCITE|mining|calcite|CALCITE|2|UNCOMMON
            enchanted_granite|Enchanted Granite|GRANITE|mining|granite|GRANITE|2|UNCOMMON
            enchanted_diorite|Enchanted Diorite|DIORITE|mining|diorite|DIORITE|2|UNCOMMON
            enchanted_andesite|Enchanted Andesite|ANDESITE|mining|andesite|ANDESITE|2|UNCOMMON
            enchanted_dripstone_block|Enchanted Dripstone Block|DRIPSTONE_BLOCK|mining|dripstone|DRIPSTONE_BLOCK|2|UNCOMMON
            enchanted_mossy_cobblestone|Enchanted Mossy Cobblestone|MOSSY_COBBLESTONE|mining|mossy_cobblestone|MOSSY_COBBLESTONE|2|UNCOMMON
            enchanted_netherrack|Enchanted Netherrack|NETHERRACK|mining|netherrack|NETHERRACK|2|UNCOMMON
            enchanted_crimson_nylium|Enchanted Crimson Nylium|CRIMSON_NYLIUM|mining|crimson_nylium|CRIMSON_NYLIUM|2|UNCOMMON
            enchanted_warped_nylium|Enchanted Warped Nylium|WARPED_NYLIUM|mining|warped_nylium|WARPED_NYLIUM|2|UNCOMMON
            enchanted_soul_sand|Enchanted Soul Sand|SOUL_SAND|mining|soul_sand|SOUL_SAND|2|UNCOMMON
            enchanted_soul_soil|Enchanted Soul Soil|SOUL_SOIL|mining|soul_soil|SOUL_SOIL|2|UNCOMMON
            enchanted_end_stone|Enchanted End Stone|END_STONE|mining|end_stone|END_STONE|3|UNCOMMON
            enchanted_obsidian|Enchanted Obsidian|OBSIDIAN|mining|obsidian|OBSIDIAN|3|UNCOMMON
            enchanted_crying_obsidian|Enchanted Crying Obsidian|CRYING_OBSIDIAN|mining|obsidian|CRYING_OBSIDIAN|4|RARE
            enchanted_oak_log|Enchanted Oak Log|OAK_LOG|foraging|oak_log|OAK_LOG|2|UNCOMMON
            enchanted_spruce_log|Enchanted Spruce Log|SPRUCE_LOG|foraging|spruce_log|SPRUCE_LOG|2|UNCOMMON
            enchanted_birch_log|Enchanted Birch Log|BIRCH_LOG|foraging|birch_log|BIRCH_LOG|2|UNCOMMON
            enchanted_jungle_log|Enchanted Jungle Log|JUNGLE_LOG|foraging|jungle_log|JUNGLE_LOG|2|UNCOMMON
            enchanted_acacia_log|Enchanted Acacia Log|ACACIA_LOG|foraging|acacia_log|ACACIA_LOG|2|UNCOMMON
            enchanted_dark_oak_log|Enchanted Dark Oak Log|DARK_OAK_LOG|foraging|dark_oak_log|DARK_OAK_LOG|2|UNCOMMON
            enchanted_mangrove_log|Enchanted Mangrove Log|MANGROVE_LOG|foraging|mangrove_log|MANGROVE_LOG|2|UNCOMMON
            enchanted_cherry_log|Enchanted Cherry Log|CHERRY_LOG|foraging|cherry_log|CHERRY_LOG|2|UNCOMMON
            enchanted_crimson_stem|Enchanted Crimson Stem|CRIMSON_STEM|foraging|crimson_stem|CRIMSON_STEM|2|UNCOMMON
            enchanted_warped_stem|Enchanted Warped Stem|WARPED_STEM|foraging|warped_stem|WARPED_STEM|2|UNCOMMON
            enchanted_bamboo_block|Enchanted Bamboo Block|BAMBOO_BLOCK|foraging|bamboo|BAMBOO_BLOCK|2|UNCOMMON
            enchanted_wheat|Enchanted Wheat|WHEAT|farming|wheat|WHEAT|2|UNCOMMON
            enchanted_hay_block|Enchanted Hay Block|HAY_BLOCK|farming|wheat|enchanted_wheat|5|RARE
            enchanted_carrot|Enchanted Carrot|CARROT|farming|carrot|CARROT|2|UNCOMMON
            enchanted_potato|Enchanted Potato|POTATO|farming|potato|POTATO|2|UNCOMMON
            enchanted_beetroot|Enchanted Beetroot|BEETROOT|farming|beetroot|BEETROOT|2|UNCOMMON
            enchanted_pumpkin|Enchanted Pumpkin|PUMPKIN|farming|pumpkin|PUMPKIN|2|UNCOMMON
            enchanted_melon_slice|Enchanted Melon Slice|MELON_SLICE|farming|melon|MELON_SLICE|2|UNCOMMON
            enchanted_melon|Enchanted Melon|MELON|farming|melon|enchanted_melon_slice|5|RARE
            enchanted_sugar_cane|Enchanted Sugar Cane|SUGAR_CANE|farming|sugar_cane|SUGAR_CANE|2|UNCOMMON
            enchanted_cactus|Enchanted Cactus|CACTUS|farming|cactus|CACTUS|2|UNCOMMON
            enchanted_cocoa_beans|Enchanted Cocoa Beans|COCOA_BEANS|farming|cocoa_beans|COCOA_BEANS|2|UNCOMMON
            enchanted_nether_wart|Enchanted Nether Wart|NETHER_WART|farming|nether_wart|NETHER_WART|2|UNCOMMON
            enchanted_brown_mushroom|Enchanted Brown Mushroom|BROWN_MUSHROOM|farming|brown_mushroom|BROWN_MUSHROOM|2|UNCOMMON
            enchanted_red_mushroom|Enchanted Red Mushroom|RED_MUSHROOM|farming|red_mushroom|RED_MUSHROOM|2|UNCOMMON
            enchanted_cod|Enchanted Cod|COD|fishing|cod|COD|2|UNCOMMON
            enchanted_salmon|Enchanted Salmon|SALMON|fishing|salmon|SALMON|2|UNCOMMON
            enchanted_tropical_fish|Enchanted Tropical Fish|TROPICAL_FISH|fishing|tropical_fish|TROPICAL_FISH|2|UNCOMMON
            enchanted_pufferfish|Enchanted Pufferfish|PUFFERFISH|fishing|pufferfish|PUFFERFISH|2|UNCOMMON
            enchanted_prismarine_shard|Enchanted Prismarine Shard|PRISMARINE_SHARD|fishing|prismarine|PRISMARINE_SHARD|3|UNCOMMON
            enchanted_prismarine_crystals|Enchanted Prismarine Crystals|PRISMARINE_CRYSTALS|fishing|prismarine|PRISMARINE_CRYSTALS|3|UNCOMMON
            enchanted_nautilus_shell|Enchanted Nautilus Shell|NAUTILUS_SHELL|fishing|nautilus_shell|NAUTILUS_SHELL|4|RARE
            enchanted_clay_ball|Enchanted Clay Ball|CLAY_BALL|fishing|clay_ball|CLAY_BALL|2|UNCOMMON
            enchanted_rotten_flesh|Enchanted Rotten Flesh|ROTTEN_FLESH|combat|rotten_flesh|ROTTEN_FLESH|2|UNCOMMON
            enchanted_bone|Enchanted Bone|BONE|combat|bone|BONE|2|UNCOMMON
            enchanted_string|Enchanted String|STRING|combat|string|STRING|2|UNCOMMON
            enchanted_spider_eye|Enchanted Spider Eye|SPIDER_EYE|combat|spider_eye|SPIDER_EYE|2|UNCOMMON
            enchanted_gunpowder|Enchanted Gunpowder|GUNPOWDER|combat|gunpowder|GUNPOWDER|2|UNCOMMON
            enchanted_slime_ball|Enchanted Slime Ball|SLIME_BALL|combat|slime_ball|SLIME_BALL|2|UNCOMMON
            enchanted_blaze_rod|Enchanted Blaze Rod|BLAZE_ROD|combat|blaze_rod|BLAZE_ROD|3|UNCOMMON
            enchanted_ender_pearl|Enchanted Ender Pearl|ENDER_PEARL|combat|ender_pearl|ENDER_PEARL|3|UNCOMMON
            enchanted_ghast_tear|Enchanted Ghast Tear|GHAST_TEAR|combat|ghast_tear|GHAST_TEAR|4|RARE
            enchanted_magma_cream|Enchanted Magma Cream|MAGMA_CREAM|combat|magma_cream|MAGMA_CREAM|3|UNCOMMON
            enchanted_phantom_membrane|Enchanted Phantom Membrane|PHANTOM_MEMBRANE|combat|phantom_membrane|PHANTOM_MEMBRANE|3|UNCOMMON
            """;

    private static final String COLLECTION_MATERIAL_UPGRADES = """
            polished_coal_core|rough_coal_chunk|MINING
            dense_iron_fragment|iron_nugget_cluster|MINING
            refined_iron_plate|dense_iron_fragment|MINING
            charged_copper_coil|copper_wire_bundle|MINING
            royal_gold_plate|gold_dust|MINING
            enchanted_lapis_shard|lapis_powder|MINING
            unstable_redstone_core|redstone_spark|MINING
            flawless_emerald|emerald_chip|MINING
            enchanted_oak_resin|oak_bark_strip|FORAGING
            pale_birch_sap|birch_bark_strip|FORAGING
            hardened_spruce_resin|spruce_pinecone|FORAGING
            jungle_heartwood|jungle_vine_fiber|FORAGING
            sunbaked_acacia_bark|acacia_thorn|FORAGING
            ancient_dark_oak_core|dark_oak_root|FORAGING
            mangrove_sap_cluster|mangrove_root_fiber|FORAGING
            dense_hay_fiber|golden_wheat_bundle|FARMING
            sweet_carrot_bundle|carrot_seed_core|FARMING
            sturdy_potato_sack|potato_sprout|FARMING
            rich_beetroot_paste|beetroot_extract|FARMING
            glowing_pumpkin_seed|pumpkin_pulp|FARMING
            juicy_melon_core|melon_rind|FARMING
            compressed_sugar_crystal|sugar_cane_fiber|FARMING
            """;

    public static void apply(ConfigurationSection root) {
        enchantedMaterialDefaults(root);
        enchantedMaterialRecipes(root);
        recipe(root, "enchanted_bread", "&aEnchanted Bread", "FARMING", "ENCHANTED_BREAD", "wheat", 2,
                List.of("WWW", "WBW", "WWW"), Map.of('W', "WHEAT", 'B', "BREAD"));
        multiRecipe(root, "super_compactor_3000", "&9Super Compactor 3000", "MINING", "SUPER_COMPACTOR_3000",
                Map.of("cobblestone", 7, "redstone", 5), List.of("CCC", "CRC", "CCC"), Map.of('C', "ENCHANTED_COBBLESTONE", 'R', "ENCHANTED_REDSTONE_BLOCK"));

        recipe(root, "wheat_bundle", "&aWheat Bundle", "FARMING", "HAY_BLOCK", "wheat", 1,
                List.of("WWW", "WWW", "WWW"), Map.of('W', "WHEAT"));
        recipe(root, "farmers_cookie", "&aFarmer's Cookie", "FARMING", "COOKIE", "wheat", 3,
                List.of("WWS", "WCS", "WWS"), Map.of('W', "WHEAT", 'S', "SUGAR", 'C', "COCOA_BEANS"));
        recipe(root, "compact_carrot", "&aCompact Carrot", "FARMING", "GOLDEN_CARROT", "carrot", 2,
                List.of("CCC", "CGC", "CCC"), Map.of('C', "CARROT", 'G', "GOLD_NUGGET"));
        recipe(root, "compact_potato", "&aCompact Potato", "FARMING", "BAKED_POTATO", "potato", 2,
                List.of("PPP", "PCP", "PPP"), Map.of('P', "POTATO", 'C', "CHARCOAL"));
        recipe(root, "potato_crate", "&aPotato Crate", "FARMING", "CHEST", "potato", 4,
                List.of("PPP", "PCP", "PPP"), Map.of('P', "POTATO", 'C', "CHEST"));
        recipe(root, "pumpkin_pie_stack", "&aPumpkin Pie Stack", "FARMING", "PUMPKIN_PIE", "pumpkin", 2,
                List.of("PPP", "SES", "PPP"), Map.of('P', "PUMPKIN", 'S', "SUGAR", 'E', "EGG"));
        recipe(root, "pumpkin_lantern", "&6Pumpkin Lantern", "FARMING", "JACK_O_LANTERN", "pumpkin", 4,
                List.of("PPP", "PTP", "PPP"), Map.of('P', "PUMPKIN", 'T', "TORCH"));
        recipe(root, "melon_smoothie", "&aMelon Smoothie", "FARMING", "HONEY_BOTTLE", "melon", 2,
                List.of("MMM", "MSM", "BBB"), Map.of('M', "MELON_SLICE", 'S', "SUGAR", 'B', "GLASS_BOTTLE"));
        recipe(root, "sugar_cane_bundle", "&aSugar Cane Bundle", "FARMING", "PAPER", "sugar_cane", 2,
                List.of("SSS", "SSS", "SSS"), Map.of('S', "SUGAR_CANE"));
        recipe(root, "cocoa_cookie_box", "&6Cookie Box", "FARMING", "COOKIE", "cocoa_beans", 3,
                List.of("CCC", "CWC", "CCC"), Map.of('C', "COCOA_BEANS", 'W', "WHEAT"));

        recipe(root, "compact_cobblestone", "&7Compact Cobblestone", "MINING", "STONE", "cobblestone", 1,
                List.of("CCC", "CCC", "CCC"), Map.of('C', "COBBLESTONE"));
        recipe(root, "reinforced_stone", "&7Reinforced Stone", "MINING", "STONE_BRICKS", "cobblestone", 3,
                List.of("SIS", "ISI", "SIS"), Map.of('S', "STONE", 'I', "IRON_NUGGET"));
        recipe(root, "redstone_cell", "&cRedstone Cell", "MINING", "REDSTONE_BLOCK", "redstone", 2,
                List.of("RRR", "RIR", "RRR"), Map.of('R', "REDSTONE", 'I', "IRON_INGOT"));
        recipe(root, "lapis_core", "&9Lapis Core", "MINING", "LAPIS_BLOCK", "lapis", 2,
                List.of("LLL", "LDL", "LLL"), Map.of('L', "LAPIS_LAZULI", 'D', "DIAMOND"));
        recipe(root, "coal_briquette", "&8Coal Briquette", "MINING", "COAL_BLOCK", "coal", 2,
                List.of("CCC", "CFC", "CCC"), Map.of('C', "COAL", 'F', "FLINT"));
        recipe(root, "iron_plate", "&fIron Plate", "MINING", "HEAVY_WEIGHTED_PRESSURE_PLATE", "iron", 2,
                List.of("III", "ISI", "III"), Map.of('I', "IRON_INGOT", 'S', "SMOOTH_STONE"));
        recipe(root, "gold_plate", "&6Gold Plate", "MINING", "LIGHT_WEIGHTED_PRESSURE_PLATE", "gold", 2,
                List.of("GGG", "GSG", "GGG"), Map.of('G', "GOLD_INGOT", 'S', "SMOOTH_STONE"));
        recipe(root, "diamond_fragment", "&bDiamond Fragment", "MINING", "DIAMOND", "diamond", 2,
                List.of("LLL", "LCL", "LLL"), Map.of('L', "LAPIS_LAZULI", 'C', "COAL_BLOCK"));
        recipe(root, "emerald_cluster", "&aEmerald Cluster", "MINING", "EMERALD_BLOCK", "emerald", 3,
                List.of("EEE", "EDE", "EEE"), Map.of('E', "EMERALD", 'D', "DIAMOND"));
        recipe(root, "obsidian_plate", "&5Obsidian Plate", "MINING", "OBSIDIAN", "obsidian", 2,
                List.of("OOO", "OIO", "OOO"), Map.of('O', "OBSIDIAN", 'I', "IRON_BLOCK"));

        recipe(root, "rotten_charm", "&2Rotten Charm", "COMBAT", "ROTTEN_FLESH", "rotten_flesh", 2,
                List.of("RRR", "RBR", "RRR"), Map.of('R', "ROTTEN_FLESH", 'B', "BONE"));
        recipe(root, "bone_blade", "&fBone Blade", "WEAPONS", "STONE_SWORD", "bone", 2,
                List.of(" B ", " B ", " S "), Map.of('B', "BONE", 'S', "STICK"));
        recipe(root, "spider_silk", "&fSpider Silk", "COMBAT", "WHITE_WOOL", "string", 2,
                List.of("SSS", "SSS", "SSS"), Map.of('S', "STRING"));
        recipe(root, "string_bow", "&aString Bow", "WEAPONS", "BOW", "string", 3,
                List.of(" ST", "S T", " ST"), Map.of('S', "STRING", 'T', "STICK"));
        recipe(root, "gunpowder_charge", "&8Gunpowder Charge", "COMBAT", "FIRE_CHARGE", "gunpowder", 2,
                List.of("GGG", "GCG", "GGG"), Map.of('G', "GUNPOWDER", 'C', "CHARCOAL"));
        recipe(root, "ender_pearl_matrix", "&5Ender Pearl Matrix", "COMBAT", "ENDER_EYE", "ender_pearl", 3,
                List.of("PPP", "PBP", "PPP"), Map.of('P', "ENDER_PEARL", 'B', "BLAZE_POWDER"));
        recipe(root, "blaze_core", "&6Blaze Core", "COMBAT", "BLAZE_POWDER", "blaze_rod", 2,
                List.of("RRR", "RFR", "RRR"), Map.of('R', "BLAZE_ROD", 'F', "FIRE_CHARGE"));
        recipe(root, "combat_talisman", "&cCombat Talisman", "ACCESSORIES", "REDSTONE_TORCH", "bone", 5,
                List.of("BFB", "FRF", "BFB"), Map.of('B', "BONE", 'F', "ROTTEN_FLESH", 'R', "REDSTONE"));

        recipe(root, "oak_bundle", "&6Oak Bundle", "FORAGING", "OAK_WOOD", "oak_log", 1,
                List.of("OOO", "OOO", "OOO"), Map.of('O', "OAK_LOG"));
        recipe(root, "spruce_bundle", "&6Spruce Bundle", "FORAGING", "SPRUCE_WOOD", "spruce_log", 1,
                List.of("SSS", "SSS", "SSS"), Map.of('S', "SPRUCE_LOG"));
        recipe(root, "birch_bundle", "&fBirch Bundle", "FORAGING", "BIRCH_WOOD", "birch_log", 1,
                List.of("BBB", "BBB", "BBB"), Map.of('B', "BIRCH_LOG"));
        recipe(root, "jungle_bundle", "&aJungle Bundle", "FORAGING", "JUNGLE_WOOD", "jungle_log", 1,
                List.of("JJJ", "JJJ", "JJJ"), Map.of('J', "JUNGLE_LOG"));
        recipe(root, "acacia_bundle", "&6Acacia Bundle", "FORAGING", "ACACIA_WOOD", "acacia_log", 1,
                List.of("AAA", "AAA", "AAA"), Map.of('A', "ACACIA_LOG"));
        recipe(root, "dark_oak_bundle", "&8Dark Oak Bundle", "FORAGING", "DARK_OAK_WOOD", "dark_oak_log", 1,
                List.of("DDD", "DDD", "DDD"), Map.of('D', "DARK_OAK_LOG"));
        recipe(root, "forest_charm", "&2Forest Charm", "ACCESSORIES", "FERN", "oak_log", 4,
                List.of("OSO", "SLS", "OSO"), Map.of('O', "OAK_LOG", 'S', "SPRUCE_LOG", 'L', "OAK_LEAVES"));

        recipe(root, "cod_bundle", "&bCod Bundle", "FISHING", "COD", "cod", 1,
                List.of("CCC", "CCC", "CCC"), Map.of('C', "COD"));
        recipe(root, "salmon_bundle", "&bSalmon Bundle", "FISHING", "SALMON", "salmon", 1,
                List.of("SSS", "SSS", "SSS"), Map.of('S', "SALMON"));
        recipe(root, "prismarine_hook", "&3Prismarine Hook", "FISHING", "PRISMARINE_SHARD", "prismarine", 2,
                List.of("PPP", "PIP", "PPP"), Map.of('P', "PRISMARINE_SHARD", 'I', "IRON_INGOT"));
        recipe(root, "pufferfish_bait", "&ePufferfish Bait", "FISHING", "PUFFERFISH", "pufferfish", 2,
                List.of("PPP", "PSP", "PPP"), Map.of('P', "PUFFERFISH", 'S', "STRING"));
        recipe(root, "ink_sack", "&8Ink Sack", "SACKS", "BLACK_DYE", "ink_sac", 2,
                List.of("III", "IBI", "III"), Map.of('I', "INK_SAC", 'B', "BUNDLE"));
        recipe(root, "sea_lantern_core", "&bSea Lantern Core", "FISHING", "SEA_LANTERN", "prismarine", 4,
                List.of("PPP", "PGP", "PPP"), Map.of('P', "PRISMARINE_CRYSTALS", 'G', "GLOWSTONE"));
        recipe(root, "anglers_charm", "&3Angler's Charm", "ACCESSORIES", "NAUTILUS_SHELL", "cod", 5,
                List.of("CSC", "SPS", "CSC"), Map.of('C', "COD", 'S', "STRING", 'P', "PRISMARINE_SHARD"));

        recipe(root, "small_farming_sack", "&aSmall Farming Sack", "SACKS", "BUNDLE", "wheat", 4,
                List.of("LWL", "WCW", "LWL"), Map.of('L', "LEATHER", 'W', "WHEAT", 'C', "CHEST"));
        recipe(root, "small_mining_sack", "&7Small Mining Sack", "SACKS", "BUNDLE", "cobblestone", 4,
                List.of("LCL", "CIC", "LCL"), Map.of('L', "LEATHER", 'C', "COBBLESTONE", 'I', "IRON_INGOT"));
        recipe(root, "farming_helmet", "&aFarming Helmet", "ARMOR", "LEATHER_HELMET", "wheat", 5,
                List.of("WWW", "W W", "   "), Map.of('W', "WHEAT"));
        recipe(root, "miner_boots", "&7Miner Boots", "ARMOR", "IRON_BOOTS", "iron", 4,
                List.of("   ", "I I", "I I"), Map.of('I', "IRON_INGOT"));
        recipe(root, "starter_dagger", "&fStarter Dagger", "WEAPONS", "IRON_SWORD", "iron", 3,
                List.of(" I ", " I ", " S "), Map.of('I', "IRON_INGOT", 'S', "STICK"));
        recipe(root, "harvest_ring", "&aHarvest Ring", "ACCESSORIES", "WHEAT_SEEDS", "wheat", 6,
                List.of("WCW", "C C", "WCW"), Map.of('W', "WHEAT", 'C', "CARROT"));
        recipe(root, "magnetic_talisman", "&bMagnetic Talisman", "ACCESSORIES", "COMPASS", "redstone", 4,
                List.of("RIR", "ICI", "RIR"), Map.of('R', "REDSTONE", 'I', "IRON_INGOT", 'C', "COMPASS"));
        recipe(root, "portable_workbench", "&ePortable Workbench", "UTILITY", "CRAFTING_TABLE", "oak_log", 2,
                List.of("PPP", "PCP", "PPP"), Map.of('P', "OAK_PLANKS", 'C', "CHEST"));

        rpgFishingRecipes(root);
        collectionCustomItemRecipes(root);
        collectionMaterialUpgradeRecipes(root);
        customEnchantRecipes(root);
        normalizeCollectionCategories(root);
        ExpandedCollectionRecipeDefaults.applyItems(root);
        ExpandedCollectionRecipeDefaults.applyCollectionsAndRecipes(root);
        RequestedCollectionRecipeDefaults.apply(root);
        RequestedMaterialDefaults80.apply(root);
        MissingProgressionDefaults60.apply(root);
        MissingProgressionDefaults100.apply(root);
        MissingProgressionDefaults100B.apply(root);
        DecorativeCollectionDefaults200.apply(root);
        RequestedRecipes200Defaults.apply(root);
        CustomSwordDefaults100.apply(root);
        CustomBowDefaults100.apply(root);
        ReforgeStoneDefaults.apply(root);
        CustomFishDefaults80.apply(root);
        // Keep shared recipe IDs such as dough classified as cooking ingredients after recipe outputs run.
        ShopFoodItemDefaults.apply(root);
        FuturisticArmorDefaults.apply(root);
    }

    private static void normalizeCollectionCategories(ConfigurationSection root) {
        Map<String, List<String>> placements = Map.of(
                "mining", List.of("iron", "coal", "copper", "gold", "lapis", "redstone", "emerald", "diamond",
                        "quartz", "amethyst", "netherite", "netherite_scrap", "ancient_debris", "cobblestone",
                        "stone", "smooth_stone", "deepslate", "cobbled_deepslate", "blackstone", "basalt", "tuff",
                        "calcite", "granite", "diorite", "andesite", "dripstone", "mossy_cobblestone", "netherrack",
                        "crimson_nylium", "warped_nylium", "soul_sand", "soul_soil", "end_stone", "obsidian", "echo_shard"),
                "combat", List.of("string", "rotten_flesh", "bone", "spider_eye", "gunpowder", "ender_pearl",
                        "blaze_rod", "slime_ball", "slimeball", "magma_cream", "ghast_tear", "phantom_membrane"),
                "farming", List.of("wheat", "carrot", "potato", "beetroot", "pumpkin", "melon", "sugar_cane",
                        "cactus", "cocoa_beans", "nether_wart", "brown_mushroom", "red_mushroom", "mushroom",
                        "bamboo", "sweet_berries", "pitcher_crop", "torchflower"),
                "foraging", List.of("oak_log", "birch_log", "spruce_log", "jungle_log", "acacia_log", "dark_oak_log",
                        "mangrove_log", "cherry_log", "crimson_stem", "warped_stem"),
                "fishing", List.of("cod", "salmon", "tropical_fish", "pufferfish", "prismarine", "nautilus_shell",
                        "clay_ball", "raw_river_fish", "silver_fish_scale", "golden_fish_scale", "deep_sea_fin",
                        "pearl_fragment", "coral_fragment", "enchanted_kelp", "treasure_shell", "voidwater_scale", "stormscale_fin")
        );
        placements.forEach((category, ids) -> {
            root.set("collections.categories." + category + ".name", pretty(category) + " Collection");
            root.set("collections.categories." + category + ".icon", categoryIcon(category));
            for (String id : ids) {
                String path = "collections.items." + id;
                if (root.isConfigurationSection(path)) root.set(path + ".category", category);
            }
        });
        root.set("collections.categories.accessories.name", "Accessories Collection");
        root.set("collections.categories.accessories.icon", "EMERALD");
    }

    private static void collectionMaterialUpgradeRecipes(ConfigurationSection root) {
        for (String line : COLLECTION_MATERIAL_UPGRADES.strip().split("\\R")) {
            String[] parts = line.trim().split("\\|", -1);
            if (parts.length < 3) continue;
            customRecipe(root, parts[0], parts[2], parts[1], 1,
                    List.of("MMM", "MMM", "MMM"), Map.of('M', parts[1]));
        }
    }

    private static void enchantedMaterialDefaults(ConfigurationSection root) {
        for (String line : ENCHANTED_MATERIALS.strip().split("\\R")) {
            String[] parts = line.trim().split("\\|", -1);
            if (parts.length < 8) continue;
            String path = "custom-items.items." + normalize(parts[0]);
            String color = parts[7].equalsIgnoreCase("RARE") ? "&9" : "&a";
            root.set(path + ".name", color + parts[1]);
            root.set(path + ".material", parts[2]);
            root.set(path + ".category", "material");
            root.set(path + ".rarity", parts[7]);
            root.set(path + ".lore", List.of("&7A refined " + parts[3] + " material used", "&7to craft powerful custom items."));
            root.set(path + ".mmo.type", "MATERIAL");
            root.set(path + ".ability.type", "none");
            root.set(path + ".ability.mana-cost", 0);
            root.set(path + ".unbreakable", false);
        }
    }

    private static void enchantedMaterialRecipes(ConfigurationSection root) {
        for (String line : ENCHANTED_MATERIALS.strip().split("\\R")) {
            String[] parts = line.trim().split("\\|", -1);
            if (parts.length < 8) continue;
            String id = normalize(parts[0]);
            String source = parts[5];
            String collection = normalize(parts[4]);
            int tier = tierNumber(parts[6]);
            recipe(root, id, (parts[7].equalsIgnoreCase("RARE") ? "&9" : "&a") + parts[1], collectionCategory(parts[3]), id, collection, tier,
                    List.of("MMM", "MMM", "MMM"), Map.of('M', source));
        }
        applyStrongerCustomItemRecipes(root);
    }

    private static void applyStrongerCustomItemRecipes(ConfigurationSection root) {
        customRecipe(root, "royal_spear", "WEAPONS", "gold", 6, List.of(" G ", " I ", " C "),
                Map.of('G', "enchanted_gold_ingot", 'I', "enchanted_iron_ingot", 'C', "royal_crest"));
        customRecipe(root, "sakura_katana", "WEAPONS", "cherry_log", 5, List.of(" C ", " I ", " S "),
                Map.of('C', "enchanted_cherry_log", 'I', "enchanted_iron_ingot", 'S', "STICK"));
        customRecipe(root, "shadow_katana", "WEAPONS", "obsidian", 6, List.of(" O ", " E ", " S "),
                Map.of('O', "enchanted_obsidian", 'E', "enchanted_ender_pearl", 'S', "shadow_cloth"));
        customRecipe(root, "deepstone_pickaxe", "MINING", "cobbled_deepslate", 5, List.of("DDD", " I ", " S "),
                Map.of('D', "enchanted_cobbled_deepslate", 'I', "enchanted_iron_ingot", 'S', "STICK"));
        customRecipe(root, "crystal_hammer", "MINING", "amethyst", 6, List.of("AAA", " I ", " C "),
                Map.of('A', "enchanted_amethyst_shard", 'I', "enchanted_iron_block", 'C', "crystal_fragment"));
        customRecipe(root, "harvesting_hoe", "FARMING", "wheat", 5, List.of("WH ", " I ", " S "),
                Map.of('W', "enchanted_wheat", 'H', "enchanted_hay_block", 'I', "enchanted_iron_ingot", 'S', "STICK"));
        customRecipe(root, "frost_bow", "WEAPONS", "string", 5, List.of(" SF", "S F", " SF"),
                Map.of('S', "enchanted_string", 'F', "frozen_bone"));
        customRecipe(root, "plague_wand", "UTILITY", "spider_eye", 6, List.of(" S ", " B ", " T "),
                Map.of('S', "enchanted_spider_eye", 'B', "enchanted_blaze_rod", 'T', "toxic_vial"));
        customRecipe(root, "farmers_hoe", "FARMING", "wheat", 1, List.of(" WW", " S ", " S "),
                Map.of('W', "WHEAT", 'S', "STICK"));
        customRecipe(root, "royal_harvest_hoe", "FARMING", "wheat", 8, List.of("HHG", " I ", " S "),
                Map.of('H', "enchanted_hay_block", 'G', "enchanted_gold_ingot", 'I', "enchanted_iron_ingot", 'S', "STICK"));
        customRecipe(root, "wheat_reaper", "FARMING", "wheat", 4, List.of(" WH", " S ", " S "),
                Map.of('W', "enchanted_wheat", 'H', "GOLDEN_HOE", 'S', "STICK"));
        customRecipe(root, "carrot_digger", "FARMING", "carrot", 3, List.of(" C ", " S ", " S "),
                Map.of('C', "enchanted_carrot", 'S', "STICK"));
        customRecipe(root, "potato_spade", "FARMING", "potato", 4, List.of(" P ", " I ", " S "),
                Map.of('P', "enchanted_potato", 'I', "enchanted_iron_ingot", 'S', "STICK"));
        customRecipe(root, "beetroot_sickle", "FARMING", "beetroot", 3, List.of(" BB", " S ", " S "),
                Map.of('B', "enchanted_beetroot", 'S', "STICK"));
        customRecipe(root, "pumpkin_cleaver", "FARMING", "pumpkin", 5, List.of("PP ", "PI ", " S "),
                Map.of('P', "enchanted_pumpkin", 'I', "enchanted_iron_ingot", 'S', "STICK"));
        customRecipe(root, "melon_splitter", "FARMING", "melon", 5, List.of("MM ", "MI ", " S "),
                Map.of('M', "enchanted_melon", 'I', "enchanted_iron_ingot", 'S', "STICK"));
        customRecipe(root, "sugar_cane_cutter", "FARMING", "sugar_cane", 5, List.of(" SS", " I ", " I "),
                Map.of('S', "enchanted_sugar_cane", 'I', "enchanted_iron_ingot"));
        customRecipe(root, "cactus_knife", "FARMING", "cactus", 3, List.of(" C ", " I ", " S "),
                Map.of('C', "enchanted_cactus", 'I', "IRON_INGOT", 'S', "STICK"));
        customRecipe(root, "cocoa_clipper", "FARMING", "cocoa_beans", 4, List.of(" C ", "ISI", "   "),
                Map.of('C', "enchanted_cocoa_beans", 'I', "IRON_INGOT", 'S', "SHEARS"));
        customRecipe(root, "nether_wart_trowel", "FARMING", "nether_wart", 6, List.of(" W ", " I ", " S "),
                Map.of('W', "enchanted_nether_wart", 'I', "enchanted_netherite_scrap", 'S', "STICK"));
        customRecipe(root, "mushroom_harvester", "FARMING", "mushroom", 3, List.of("RBR", " B ", " S "),
                Map.of('R', "enchanted_red_mushroom", 'B', "enchanted_brown_mushroom", 'S', "STICK"));
        customRecipe(root, "bamboo_cutter", "FARMING", "bamboo", 3, List.of(" B ", " B ", " S "),
                Map.of('B', "enchanted_bamboo_block", 'S', "STICK"));
        customRecipe(root, "berry_pruner", "FARMING", "sweet_berries", 3, List.of(" B ", "ISI", "   "),
                Map.of('B', "SWEET_BERRIES", 'I', "IRON_INGOT", 'S', "SHEARS"));
        customRecipe(root, "pitcher_crop_spade", "FARMING", "pitcher_crop", 5, List.of(" P ", " B ", " S "),
                Map.of('P', "PITCHER_POD", 'B', "BRUSH", 'S', "STICK"));
        customRecipe(root, "torchflower_sickle", "FARMING", "torchflower", 5, List.of(" TT", " H ", " S "),
                Map.of('T', "TORCHFLOWER_SEEDS", 'H', "GOLDEN_HOE", 'S', "STICK"));
        customRecipe(root, "sprinkler", "FARMING", "wheat", 4, List.of(" C ", "BWB", " C "),
                Map.of('C', "enchanted_copper_ingot", 'B', "BUCKET", 'W', "WATER_BUCKET"));
        customRecipe(root, "advanced_sprinkler", "FARMING", "wheat", 6, List.of("CSC", "BWB", "CSC"),
                Map.of('C', "enchanted_copper_ingot", 'S', "sprinkler", 'B', "BUCKET", 'W', "WATER_BUCKET"));
        customRecipe(root, "enchanted_sprinkler", "FARMING", "wheat", 8, List.of("CAC", "SWS", "CAC"),
                Map.of('C', "enchanted_copper_block", 'A', "enchanted_amethyst_shard", 'S', "advanced_sprinkler", 'W', "WATER_BUCKET"));
        customRecipe(root, "farmers_gloves", "FARMING", "wheat", 3, List.of("LWL", "L L", "   "),
                Map.of('L', "LEATHER", 'W', "enchanted_wheat"));
        customRecipe(root, "harvester_belt", "FARMING", "wheat", 5, List.of("LHL", "W W", "LHL"),
                Map.of('L', "LEATHER", 'H', "enchanted_hay_block", 'W', "enchanted_wheat"));
        customRecipe(root, "compost_charm", "FARMING", "wheat", 5, List.of("WCW", "CBC", "WCW"),
                Map.of('W', "enchanted_wheat", 'C', "COMPOSTER", 'B', "BONE_MEAL"));
        customRecipe(root, "golden_seed_pouch", "FARMING", "wheat", 6, List.of("SGS", "GPG", "SGS"),
                Map.of('S', "WHEAT_SEEDS", 'G', "enchanted_gold_ingot", 'P', "enchanted_potato"));
    }

    private static void customRecipe(ConfigurationSection root, String id, String category, String collection, int tier, List<String> shape, Map<Character, String> ingredients) {
        String path = "recipes." + id;
        root.set(path + ".display-name", root.getString("custom-items.items." + id + ".name", "&f" + pretty(id)));
        root.set(path + ".category", category);
        root.set(path + ".result", id);
        root.set(path + ".unlock.type", "COLLECTION");
        root.set(path + ".unlock.collection", collection);
        root.set(path + ".unlock.tier", tier);
        root.set(path + ".shape", shape);
        ingredients.forEach((key, value) -> root.set(path + ".ingredients." + key, value));
    }

    private static String collectionCategory(String category) {
        return switch (normalize(category)) {
            case "farming" -> "FARMING";
            case "foraging" -> "FORAGING";
            case "fishing" -> "FISHING";
            case "combat" -> "COMBAT";
            default -> "MINING";
        };
    }

    private static void recipe(ConfigurationSection root, String id, String displayName, String category, String result, String collection, int tier, List<String> shape, Map<Character, String> ingredients) {
        String path = "recipes." + id;
        root.set(path + ".display-name", displayName);
        root.set(path + ".category", category);
        root.set(path + ".result", result);
        root.set(path + ".unlock.type", "COLLECTION");
        root.set(path + ".unlock.collection", collection);
        root.set(path + ".unlock.tier", tier);
        root.set(path + ".shape", shape);
        ingredients.forEach((key, value) -> root.set(path + ".ingredients." + key, value));
        collection(root, collection, category.toLowerCase(), ingredientMaterial(ingredients), tier, id, displayName);
    }

    private static void multiRecipe(ConfigurationSection root, String id, String displayName, String category, String result, Map<String, Integer> requirements, List<String> shape, Map<Character, String> ingredients) {
        String path = "recipes." + id;
        root.set(path + ".display-name", displayName);
        root.set(path + ".category", category);
        root.set(path + ".result", result);
        root.set(path + ".unlock.type", "MULTI_COLLECTION");
        requirements.forEach((collection, tier) -> root.set(path + ".unlock.requirements." + collection, tier));
        root.set(path + ".shape", shape);
        ingredients.forEach((key, value) -> root.set(path + ".ingredients." + key, value));
        requirements.forEach((collection, tier) -> collection(root, collection, "mining", ingredientMaterial(ingredients), tier, id, displayName));
    }

    private static void collection(ConfigurationSection root, String id, String category, String material, int tier, String recipeId, String recipeName) {
        String key = id.toLowerCase().replace('-', '_').replace(' ', '_');
        root.set("collections.categories." + category + ".name", pretty(category) + " Collection");
        root.set("collections.categories." + category + ".icon", categoryIcon(category));
        root.set("collections.items." + key + ".name", pretty(key) + " Collection");
        root.set("collections.items." + key + ".category", category);
        root.set("collections.items." + key + ".icon", material);
        root.set("collections.items." + key + ".materials", List.of(material));
        int amount = Math.max(50, tier * 50);
        String tierKey = String.valueOf(tier);
        root.set("collections.items." + key + ".unlocks." + tierKey + ".amount", amount);
        root.set("collections.items." + key + ".unlocks." + tierKey + ".reward-type", "recipe");
        root.set("collections.items." + key + ".unlocks." + tierKey + ".reward-value", recipeId);
        root.set("collections.items." + key + ".unlocks." + tierKey + ".name", recipeName);
    }

    private static void rpgFishingRecipes(ConfigurationSection root) {
        fishingRecipe(root, "simple_bait", "raw_river_fish", 1, 25,
                List.of(" F ", "FSF", " F "), Map.of('F', "raw_river_fish", 'S', "STRING"));
        fishingRecipe(root, "deep_sea_bait", "silver_fish_scale", 1, 30,
                List.of(" S ", "SRS", " S "), Map.of('S', "silver_fish_scale", 'R', "raw_river_fish"));
        fishingRecipe(root, "treasure_bait", "treasure_shell", 1, 10,
                List.of(" G ", "GSG", " G "), Map.of('G', "golden_fish_scale", 'S', "treasure_shell"));
        fishingRecipe(root, "monster_bait", "deep_sea_fin", 1, 20,
                List.of(" F ", "FBF", " F "), Map.of('F', "deep_sea_fin", 'B', "ancient_fish_bone"));
        fishingRecipe(root, "deep_sea_rod", "silver_fish_scale", 2, 100,
                List.of(" SS", " SR", "R  "), Map.of('S', "silver_fish_scale", 'R', "reinforced_rod"));
        fishingRecipe(root, "crystal_rod", "coral_fragment", 1, 75,
                List.of(" CC", " CR", "R  "), Map.of('C', "crystal_fish_shard", 'R', "deep_sea_rod"));
        fishingRecipe(root, "royal_fishing_rod", "golden_fish_scale", 1, 50,
                List.of(" GG", " GR", "R  "), Map.of('G', "royal_fish_scale", 'R', "crystal_rod"));
        fishingRecipe(root, "fishing_armor_helmet", "raw_river_fish", 2, 100,
                List.of("SSS", "S S", "   "), Map.of('S', "silver_fish_scale"));
        fishingRecipe(root, "fishing_armor_chestplate", "raw_river_fish", 3, 250,
                List.of("S S", "SSS", "SSS"), Map.of('S', "silver_fish_scale"));
        fishingRecipe(root, "fishing_armor_leggings", "raw_river_fish", 4, 500,
                List.of("SSS", "S S", "S S"), Map.of('S', "silver_fish_scale"));
        fishingRecipe(root, "fishing_armor_boots", "raw_river_fish", 5, 750,
                List.of("   ", "S S", "S S"), Map.of('S', "silver_fish_scale"));
        fishingRecipe(root, "sea_charm", "pearl_fragment", 1, 25,
                List.of("CSC", "SPS", "CSC"), Map.of('C', "coral_fragment", 'S', "silver_fish_scale", 'P', "pearl_fragment"));
        fishingRecipe(root, "angler_necklace", "deep_sea_fin", 2, 75,
                List.of(" S ", "PFP", " S "), Map.of('S', "STRING", 'P', "pearl_fragment", 'F', "deep_sea_fin"));
        fishingRecipe(root, "treasure_ring", "treasure_shell", 2, 30,
                List.of("GSG", "S S", "GSG"), Map.of('G', "golden_fish_scale", 'S', "treasure_shell"));
    }

    private static void fishingRecipe(ConfigurationSection root, String result, String collectionId, int tier,
                                      int requiredAmount, List<String> shape, Map<Character, String> ingredients) {
        String id = normalize(result);
        String path = "recipes." + id;
        String display = root.getString("custom-items.items." + id + ".name", "&b" + pretty(id));
        root.set(path + ".display-name", display);
        root.set(path + ".category", recipeCategory(root.getString("custom-items.items." + id + ".category", "fishing")));
        root.set(path + ".result", id);
        root.set(path + ".unlock.type", "COLLECTION");
        root.set(path + ".unlock.collection", collectionId);
        root.set(path + ".unlock.tier", tier);
        root.set(path + ".shape", shape);
        ingredients.forEach((key, value) -> root.set(path + ".ingredients." + key, value));

        String unlock = "collections.items." + collectionId + ".unlocks." + tier;
        root.set(unlock + ".amount", requiredAmount);
        root.set(unlock + ".reward-type", "recipe");
        root.set(unlock + ".reward-value", id);
        root.set(unlock + ".name", display);
    }

    private static void collectionCustomItemRecipes(ConfigurationSection root) {
        ConfigurationSection collections = root.getConfigurationSection("collections.items");
        if (collections == null) return;
        for (String collectionId : collections.getKeys(false)) {
            ConfigurationSection unlocks = collections.getConfigurationSection(collectionId + ".unlocks");
            if (unlocks == null) continue;
            String category = collections.getString(collectionId + ".category", inferCategory(collectionId));
            String material = firstMaterial(collections.getStringList(collectionId + ".materials"), collections.getString(collectionId + ".icon", "CHEST"));
            for (String tierKey : unlocks.getKeys(false)) {
                String customItem = normalize(unlocks.getString(tierKey + ".custom-item", ""));
                if (customItem.isBlank()) continue;
                if (root.isConfigurationSection("recipes." + customItem)) continue;
                String name = unlocks.getString(tierKey + ".name", itemName(root, customItem));
                String itemCategory = root.getString("custom-items.items." + customItem + ".category", category);
                customItemRecipe(root, customItem, name, recipeCategory(itemCategory), collectionId, tierNumber(tierKey), material, itemMaterial(root, customItem));
            }
        }
    }

    private static void customItemRecipe(ConfigurationSection root, String id, String displayName, String category, String collection, int tier, String collectionMaterial, String itemMaterial) {
        String path = "recipes." + id;
        root.set(path + ".display-name", displayName);
        root.set(path + ".category", category);
        root.set(path + ".result", id);
        root.set(path + ".unlock.type", "COLLECTION");
        root.set(path + ".unlock.collection", collection);
        root.set(path + ".unlock.tier", tier);
        List<String> shape = recipeShape(category);
        root.set(path + ".shape", shape);
        if (uses(shape, 'C')) root.set(path + ".ingredients.C", collectionMaterial);
        if (uses(shape, 'M')) root.set(path + ".ingredients.M", itemMaterial);
        if (uses(shape, 'S')) root.set(path + ".ingredients.S", "STICK");
        if (uses(shape, 'L')) root.set(path + ".ingredients.L", "LEATHER");
        if (uses(shape, 'I')) root.set(path + ".ingredients.I", "IRON_INGOT");
    }

    private static void customEnchantRecipes(ConfigurationSection root) {
        String[][] enchants = {
                {"sharpfang", "&cSharpfang", "weapon"}, {"lifesteal", "&cLifesteal", "weapon"}, {"venom", "&2Venom", "weapon"},
                {"frostbite", "&bFrostbite", "weapon"}, {"inferno", "&6Inferno", "weapon"}, {"thunderstrike", "&eThunderstrike", "weapon"},
                {"witherbrand", "&8Witherbrand", "weapon"}, {"executioner", "&4Executioner", "weapon"}, {"giant_slayer", "&dGiant Slayer", "weapon"},
                {"dragonbane", "&5Dragonbane", "weapon"}, {"enderbane", "&5Enderbane", "weapon"}, {"smitebound", "&fSmitebound", "weapon"},
                {"first_strike", "&aFirst Strike", "weapon"}, {"combo", "&6Combo", "weapon"}, {"stun", "&7Stun", "weapon"},
                {"cleave", "&cCleave", "weapon"}, {"guard", "&9Guard", "armor"}, {"stone_skin", "&7Stone Skin", "armor"},
                {"fireguard", "&6Fireguard", "armor"}, {"blastguard", "&eBlastguard", "armor"}, {"featherweight", "&fFeatherweight", "armor"},
                {"last_stand", "&cLast Stand", "armor"}, {"rejuvenate", "&aRejuvenate", "armor"}, {"swiftstep", "&bSwiftstep", "armor"},
                {"nightcloak", "&8Nightcloak", "armor"}, {"thornsoul", "&dThornsoul", "armor"}, {"prospector", "&eProspector", "tool"},
                {"autosmelt", "&6Autosmelt", "tool"}, {"veinburst", "&7Veinburst", "tool"}, {"timber", "&2Timber", "tool"},
                {"harvester", "&aHarvester", "tool"}, {"replanter", "&aReplanter", "tool"}, {"magnet", "&bMagnet", "tool"},
                {"durability", "&fDurability", "tool"}, {"scholar", "&dScholar", "tool"}, {"precision", "&ePrecision", "bow"},
                {"volley", "&aVolley", "bow"}, {"piercer", "&9Piercer", "bow"}, {"snare", "&bSnare", "bow"},
                {"flameburst", "&6Flameburst", "bow"}
        };
        root.set("collections.categories.utility.name", "Utility Collection");
        root.set("collections.categories.utility.icon", "ENCHANTED_BOOK");
        root.set("collections.items.enchanting.name", "&dEnchanting Collection");
        root.set("collections.items.enchanting.category", "utility");
        root.set("collections.items.enchanting.icon", "ENCHANTED_BOOK");
        root.set("collections.items.enchanting.materials", List.of("LAPIS_LAZULI", "EXPERIENCE_BOTTLE", "ENCHANTED_BOOK"));
        for (int index = 0; index < enchants.length; index++) {
            String id = "enchant_" + enchants[index][0] + "_book";
            String path = "recipes." + id;
            root.set(path + ".display-name", enchants[index][1] + " I Book");
            root.set(path + ".category", switch (enchants[index][2]) {
                case "weapon", "bow" -> "WEAPONS";
                case "armor" -> "ARMOR";
                case "tool" -> "UTILITY";
                default -> "UTILITY";
            });
            root.set(path + ".result", "custom_enchant:" + enchants[index][0]);
            root.set(path + ".unlock.type", "COLLECTION");
            root.set(path + ".unlock.collection", "enchanting");
            root.set(path + ".unlock.tier", index + 1);
            root.set(path + ".shape", List.of(" L ", " B ", " E "));
            root.set(path + ".ingredients.L", "LAPIS_LAZULI");
            root.set(path + ".ingredients.B", "BOOK");
            root.set(path + ".ingredients.E", "EXPERIENCE_BOTTLE");
            String tier = String.valueOf(index + 1);
            root.set("collections.items.enchanting.unlocks." + tier + ".amount", 75 + (index * 75));
            root.set("collections.items.enchanting.unlocks." + tier + ".reward-type", "recipe");
            root.set("collections.items.enchanting.unlocks." + tier + ".reward-value", id);
            root.set("collections.items.enchanting.unlocks." + tier + ".name", enchants[index][1] + " I Book");
        }
    }

    private static List<String> recipeShape(String category) {
        return switch (category) {
            case "WEAPONS" -> List.of(" C ", " M ", " S ");
            case "ARMOR" -> List.of("CMC", "CLC", "   ");
            case "FARMING", "FISHING" -> List.of("CCC", "CMC", "CCC");
            case "SACKS" -> List.of("LCL", "CMC", "LCL");
            case "ACCESSORIES" -> List.of(" C ", "CMC", " C ");
            default -> List.of("CCC", "CMC", "CCC");
        };
    }

    private static boolean uses(List<String> shape, char symbol) {
        for (String line : shape) if (line.indexOf(symbol) >= 0) return true;
        return false;
    }

    private static String itemName(ConfigurationSection root, String customItem) {
        return root.getString("custom-items.items." + customItem + ".name", "&f" + pretty(customItem));
    }

    private static String itemMaterial(ConfigurationSection root, String customItem) {
        return root.getString("custom-items.items." + customItem + ".material", "PAPER");
    }

    private static String firstMaterial(List<String> materials, String fallback) {
        List<String> usable = new ArrayList<>();
        for (String material : materials) if (material != null && !material.isBlank()) usable.add(material);
        return usable.isEmpty() ? (fallback == null || fallback.isBlank() ? "CHEST" : fallback) : usable.getFirst();
    }

    private static int tierNumber(String tier) {
        String digits = tier == null ? "" : tier.replaceAll("\\D+", "");
        if (digits.isBlank()) return 1;
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private static String recipeCategory(String itemCategory) {
        String key = normalize(itemCategory);
        if (key.contains("sword") || key.contains("weapon") || key.contains("mace") || key.contains("blade") || key.contains("bow")) return "WEAPONS";
        if (key.contains("pick") || key.contains("drill")) return "MINING";
        if (key.contains("axe") || key.contains("hoe") || key.contains("tool")) return "UTILITY";
        if (key.contains("rod")) return "FISHING";
        if (key.contains("armor") || key.contains("helmet") || key.contains("chestplate") || key.contains("leggings") || key.contains("boots")) return "ARMOR";
        if (key.contains("bait")) return "FISHING";
        if (key.contains("food") || key.contains("potion") || key.contains("farming")) return "FARMING";
        if (key.contains("accessory") || key.contains("charm") || key.contains("talisman")) return "ACCESSORIES";
        if (key.contains("sack") || key.contains("bag")) return "SACKS";
        if (key.contains("fishing")) return "FISHING";
        if (key.contains("combat")) return "COMBAT";
        if (key.contains("mining")) return "MINING";
        if (key.contains("foraging") || key.contains("wood")) return "FORAGING";
        if (key.contains("enchant") || key.contains("rune")) return "UTILITY";
        return "UTILITY";
    }

    private static String inferCategory(String collectionId) {
        String key = normalize(collectionId);
        if (key.contains("wheat") || key.contains("carrot") || key.contains("potato") || key.contains("pumpkin") || key.contains("melon")) return "farming";
        if (key.contains("log") || key.contains("wood")) return "foraging";
        if (key.contains("fish") || key.contains("cod") || key.contains("salmon")) return "fishing";
        if (key.contains("combat") || key.contains("bone") || key.contains("rotten") || key.contains("spider")) return "combat";
        return "mining";
    }

    private static String ingredientMaterial(Map<Character, String> ingredients) {
        return ingredients.values().stream().findFirst().orElse("CHEST");
    }

    private static String categoryIcon(String category) {
        return switch (category) {
            case "farming" -> "WHEAT";
            case "mining" -> "DIAMOND_PICKAXE";
            case "combat" -> "IRON_SWORD";
            case "weapons" -> "DIAMOND_SWORD";
            case "foraging" -> "OAK_LOG";
            case "fishing" -> "FISHING_ROD";
            case "sacks" -> "BUNDLE";
            case "armor" -> "DIAMOND_CHESTPLATE";
            case "accessories" -> "EMERALD";
            default -> "CRAFTING_TABLE";
        };
    }

    private static String pretty(String value) {
        String[] parts = value.replace('-', '_').split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase());
        }
        return builder.toString();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_').trim();
    }
}
