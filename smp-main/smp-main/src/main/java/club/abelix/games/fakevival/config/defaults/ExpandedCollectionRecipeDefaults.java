package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Java-only defaults for the expanded SMP material collections and their recipes. */
public final class ExpandedCollectionRecipeDefaults {
    private ExpandedCollectionRecipeDefaults() {
    }

    private static final String MATERIALS = """
            ember_fang|&6Ember Fang|BLAZE_POWDER|MONSTER_MATERIAL|combat|combat|BLAZE,MAGMA_CUBE
            frost_core|&bFrost Core|PACKED_ICE|MINING_MATERIAL|mining|mining|PACKED_ICE,BLUE_ICE
            royal_guard_emblem|&eRoyal Guard Emblem|GOLD_NUGGET|DUNGEON_MATERIAL|dungeon|combat|ZOMBIFIED_PIGLIN,PIGLIN_BRUTE
            voidsteel_ingot|&5Voidsteel Ingot|NETHERITE_INGOT|DUNGEON_MATERIAL|dungeon|combat|ENDERMAN,WITHER_SKELETON
            sun_fragment|&eSun Fragment|GLOWSTONE_DUST|MONSTER_MATERIAL|combat|combat|BLAZE,GHAST
            shadow_stinger|&8Shadow Stinger|FLINT|DUNGEON_MATERIAL|dungeon|combat|SPIDER,CAVE_SPIDER
            venom_sac|&2Venom Sac|SPIDER_EYE|MONSTER_MATERIAL|combat|combat|SPIDER,CAVE_SPIDER
            glass_fang|&fGlass Fang|QUARTZ|MINING_MATERIAL|mining|mining|AMETHYST_CLUSTER,QUARTZ_BLOCK
            crimson_needle_fragment|&cCrimson Needle Fragment|REDSTONE|MONSTER_MATERIAL|combat|combat|ZOMBIE,HUSK
            nightfall_cloth|&5Nightfall Cloth|BLACK_WOOL|DUNGEON_MATERIAL|dungeon|combat|PHANTOM,WITHER_SKELETON
            sakura_petals|&dSakura Petals|PINK_PETALS|FORAGING_MATERIAL|foraging|foraging|CHERRY_LOG,CHERRY_LEAVES
            storm_essence|&9Storm Essence|LIGHTNING_ROD|RECIPE_MATERIAL|magic|combat|CREEPER,ENDERMAN
            oni_horn|&4Oni Horn|GOAT_HORN|DUNGEON_MATERIAL|dungeon|combat|RAVAGER,HOGLIN
            moon_shard|&bMoon Shard|PRISMARINE_CRYSTALS|RECIPE_MATERIAL|magic|combat|PHANTOM,ENDERMAN
            bamboo_fiber|&aBamboo Fiber|BAMBOO|FORAGING_MATERIAL|foraging|farming|BAMBOO,BAMBOO_BLOCK
            wind_feather|&fWind Feather|FEATHER|FORAGING_MATERIAL|foraging|combat|PHANTOM,CHICKEN
            ember_string|&6Ember String|STRING|MONSTER_MATERIAL|combat|combat|STRIDER,BLAZE
            frozen_hide|&bFrozen Hide|RABBIT_HIDE|FISHING_MATERIAL|fishing|fishing|COD,SALMON
            royal_bolt_head|&eRoyal Bolt Head|IRON_NUGGET|DUNGEON_MATERIAL|dungeon|combat|PILLAGER,PIGLIN
            void_arrowhead|&5Void Arrowhead|ECHO_SHARD|DUNGEON_MATERIAL|dungeon|combat|ENDERMAN,SHULKER
            spark_dust|&eSpark Dust|GLOWSTONE_DUST|RECIPE_MATERIAL|magic|combat|CREEPER
            frost_dust|&bFrost Dust|SNOWBALL|RECIPE_MATERIAL|magic|mining|SNOW_BLOCK,POWDER_SNOW
            toxic_dust|&2Toxic Dust|GREEN_DYE|RECIPE_MATERIAL|magic|combat|CAVE_SPIDER,SLIME
            void_dust|&5Void Dust|BLACK_DYE|DUNGEON_MATERIAL|dungeon|combat|ENDERMAN,SHULKER
            sun_dust|&6Sun Dust|YELLOW_DYE|RECIPE_MATERIAL|magic|combat|BLAZE,GHAST
            apprentice_rune|&aApprentice Rune|LAPIS_LAZULI|RECIPE_MATERIAL|magic|mining|LAPIS_ORE,DEEPSLATE_LAPIS_ORE
            storm_rune|&9Storm Rune|LIGHT_BLUE_DYE|RECIPE_MATERIAL|magic|combat|CREEPER,ENDERMAN
            necrotic_bone|&8Necrotic Bone|BONE|DUNGEON_MATERIAL|dungeon|combat|WITHER_SKELETON,SKELETON
            crystal_focus|&dCrystal Focus|AMETHYST_CLUSTER|MINING_MATERIAL|mining|mining|AMETHYST_CLUSTER,BUDDING_AMETHYST
            infernal_core|&cInfernal Core|MAGMA_CREAM|MONSTER_MATERIAL|combat|combat|MAGMA_CUBE,BLAZE
            healing_herb|&aHealing Herb|FERN|FARMING_MATERIAL|alchemy|farming|WHEAT,CARROTS
            mana_leaf|&bMana Leaf|AZALEA_LEAVES|FARMING_MATERIAL|alchemy|foraging|AZALEA_LEAVES,FLOWERING_AZALEA_LEAVES
            iron_scale|&7Iron Scale|IRON_NUGGET|MINING_MATERIAL|alchemy|mining|IRON_ORE,DEEPSLATE_IRON_ORE
            swift_leaf|&aSwift Leaf|OAK_LEAVES|FORAGING_MATERIAL|alchemy|foraging|OAK_LEAVES,BIRCH_LEAVES
            honey_glaze|&6Honey Glaze|HONEYCOMB|FARMING_MATERIAL|farming|farming|BEE_NEST,BEEHIVE
            spiced_mushroom|&cSpiced Mushroom|RED_MUSHROOM|FARMING_MATERIAL|farming|farming|RED_MUSHROOM,BROWN_MUSHROOM
            fisherman_spice|&3Fisherman Spice|KELP|FISHING_MATERIAL|fishing|fishing|COD,SALMON
            miners_crust|&6Miner's Crust|BROWN_DYE|MINING_MATERIAL|mining|mining|STONE,DEEPSLATE
            forest_berries|&dForest Berries|SWEET_BERRIES|FORAGING_MATERIAL|foraging|foraging|SWEET_BERRY_BUSH,OAK_LEAVES
            royal_plate|&eRoyal Plate|GOLDEN_PRESSURE_PLATE|FARMING_MATERIAL|farming|farming|WHEAT,PUMPKIN
            luck_fragment|&6Luck Fragment|RAW_GOLD|MINING_MATERIAL|mining|mining|GOLD_ORE,DEEPSLATE_GOLD_ORE
            blessed_seed|&aBlessed Seed|WHEAT_SEEDS|FARMING_MATERIAL|farming|farming|WHEAT,CARROTS
            pearl_scale|&bPearl Scale|PRISMARINE_SHARD|FISHING_MATERIAL|fishing|fishing|COD,SALMON
            shadow_luck_shard|&5Shadow Luck Shard|AMETHYST_SHARD|DUNGEON_MATERIAL|dungeon|combat|ENDERMAN,WITHER_SKELETON
            warrior_focus_shard|&cWarrior Focus Shard|REDSTONE|MONSTER_MATERIAL|combat|combat|ZOMBIE,SKELETON
            ancient_guard_plate|&eAncient Guard Plate|HEAVY_WEIGHTED_PRESSURE_PLATE|DUNGEON_MATERIAL|dungeon|combat|IRON_GOLEM,RAVAGER
            vigor_wood|&aVigor Wood|OAK_WOOD|FORAGING_MATERIAL|accessories|foraging|OAK_LOG,OAK_WOOD
            mana_thread|&bMana Thread|LIGHT_BLUE_WOOL|RECIPE_MATERIAL|magic|combat|WITCH,ENDERMAN
            battle_fury_shard|&cBattle Fury Shard|REDSTONE_TORCH|MONSTER_MATERIAL|combat|combat|PIGLIN_BRUTE,RAVAGER
            deep_mine_relic|&6Deep Mine Relic|RAW_GOLD_BLOCK|MINING_MATERIAL|mining|mining|DEEPSLATE,ANCIENT_DEBRIS
            ancient_light_shard|&eAncient Light Shard|GLOWSTONE|DUNGEON_MATERIAL|dungeon|combat|BLAZE,GUARDIAN
            void_resistance_shard|&5Void Resistance Shard|PURPUR_BLOCK|DUNGEON_MATERIAL|dungeon|combat|SHULKER,ENDERMAN
            bronze_chain|&6Bronze Chain|CHAIN|RECIPE_MATERIAL|accessories|mining|COPPER_ORE,DEEPSLATE_COPPER_ORE
            crystal_heart|&dCrystal Heart|AMETHYST_CLUSTER|MINING_MATERIAL|accessories|mining|AMETHYST_CLUSTER,BUDDING_AMETHYST
            hunter_fang|&cHunter Fang|RABBIT_FOOT|MONSTER_MATERIAL|combat|combat|WOLF,SPIDER
            royal_sun_gem|&eRoyal Sun Gem|SUNFLOWER|RECIPE_MATERIAL|accessories|combat|BLAZE,PIGLIN_BRUTE
            shadow_chain_link|&5Shadow Chain Link|CHAIN|DUNGEON_MATERIAL|dungeon|combat|WITHER_SKELETON,ENDERMAN
            ascended_star_fragment|&dAscended Star Fragment|NETHER_STAR|DUNGEON_MATERIAL|accessories|combat|WITHER,ENDER_DRAGON
            deep_sea_scale|&3Deep Sea Scale|PRISMARINE_CRYSTALS|FISHING_MATERIAL|fishing|fishing|COD,SALMON
            treasure_fragment|&6Treasure Fragment|GOLD_NUGGET|FISHING_MATERIAL|fishing|fishing|COD,SALMON
            golden_harvest_fragment|&eGolden Harvest Fragment|GOLD_NUGGET|FARMING_MATERIAL|farming|farming|WHEAT,CARROTS
            ancient_tool_relic|&6Ancient Tool Relic|NETHERITE_SCRAP|MINING_MATERIAL|tools|mining|ANCIENT_DEBRIS,DEEPSLATE
            """;

    private static final String EXTRA_OUTPUTS = """
            miners_ring|&6Miner's Ring|GOLD_NUGGET|RING|RARE|rings|A ring carried by deep-mine prospectors.|MINING_FORTUNE=12;DEFENSE=5
            farmers_ring|&aFarmer's Ring|WHEAT_SEEDS|RING|RARE|rings|A ring blessed for reliable harvests.|FARMING_FORTUNE=12;HEALTH=10
            fishermans_ring|&bFisherman's Ring|PRISMARINE_SHARD|RING|RARE|rings|A ring polished with pearl scales.|FISHING_FORTUNE=12;SEA_CREATURE_CHANCE=2
            warriors_ring|&cWarrior's Ring|IRON_INGOT|RING|RARE|rings|A focused ring for practiced warriors.|STRENGTH=12;CRIT_CHANCE=3
            mana_ring|&bMana Ring|LAPIS_LAZULI|RING|RARE|rings|A woven ring that improves mana flow.|MANA=45;MANA_REGEN=3
            coral_fishing_rod|&dCoral Fishing Rod|FISHING_ROD|FISHING_ROD|RARE|fishing|A colorful rod reinforced with coral.|FISHING_FORTUNE=18;SEA_CREATURE_CHANCE=2
            deep_sea_rod|&3Deep Sea Rod|FISHING_ROD|FISHING_ROD|EPIC|fishing|A durable rod made for deep waters.|FISHING_FORTUNE=30;SEA_CREATURE_CHANCE=5
            angler_charm|&bAngler Charm|COD|CHARM|RARE|fishing|A seasoned angler's lucky charm.|FISHING_FORTUNE=15
            sea_creature_totem|&3Sea Creature Totem|TOTEM_OF_UNDYING|TOTEM|EPIC|fishing|A totem that attracts unusual sea creatures.|SEA_CREATURE_CHANCE=8;FISHING_FORTUNE=20
            treasure_hook|&6Treasure Hook|TRIPWIRE_HOOK|FISHING_ROD|EPIC|fishing|A hook balanced for sunken treasure.|FISHING_FORTUNE=25;MAGIC_FIND=5
            harvesters_gloves|&aHarvester's Gloves|LEATHER|FARMING_EQUIPMENT|RARE|farming_equipment|Gloves made for long harvests.|FARMING_FORTUNE=18;SPEED=3
            farmers_belt|&6Farmer's Belt|LEATHER|FARMING_EQUIPMENT|RARE|farming_equipment|A sturdy belt for farming equipment.|FARMING_FORTUNE=20;HEALTH=15
            crop_growth_charm|&aCrop Growth Charm|WHEAT_SEEDS|CHARM|EPIC|farming_equipment|A charm humming with growing energy.|FARMING_FORTUNE=25
            golden_harvest_hoe|&eGolden Harvest Hoe|GOLDEN_HOE|FARMING_EQUIPMENT|EPIC|farming_equipment|A golden hoe made for valuable crops.|FARMING_FORTUNE=35;SPEED=5
            sugar_cane_cutter|&aSugar Cane Cutter|IRON_HOE|FARMING_EQUIPMENT|EPIC|farming_equipment|A specialized cutter for tall cane.|FARMING_FORTUNE=30;SPEED=5
            crystal_pickaxe|&dCrystal Pickaxe|DIAMOND_PICKAXE|TOOL|EPIC|tools|A focused pickaxe for crystal seams.|DAMAGE=35;MINING_FORTUNE=30
            deep_mine_drill|&6Deep Mine Drill|NETHERITE_PICKAXE|TOOL|LEGENDARY|tools|A heavy drill assembled from mine relics.|DAMAGE=45;MINING_FORTUNE=50
            lumber_axe|&2Lumber Axe|IRON_AXE|TOOL|RARE|tools|A broad axe for dense timber.|DAMAGE=30;FORAGING_FORTUNE=25
            storm_shovel|&9Storm Shovel|IRON_SHOVEL|TOOL|EPIC|tools|A light shovel charged by storm essence.|DAMAGE=25;MINING_FORTUNE=20;SPEED=5
            multi_tool_of_the_ancients|&6Multi-Tool of the Ancients|NETHERITE_PICKAXE|TOOL|LEGENDARY|tools|An ancient tool built for every expedition.|DAMAGE=60;MINING_FORTUNE=45;FORAGING_FORTUNE=35;FARMING_FORTUNE=25
            """;

    private static final String REQUESTED_DUNGEON_MATERIALS = """
            imperial_cloth|&fImperial Cloth|WHITE_WOOL|DUNGEON_MATERIAL|COMMON|A torn cloth from an Imperial dungeon soldier.
            imperial_medal|&6Imperial Medal|GOLD_NUGGET|DUNGEON_MATERIAL|RARE|A medal carried by high-ranking Imperial guards.
            imperial_bone|&fImperial Bone|BONE|DUNGEON_MATERIAL|COMMON|An old bone from the Imperial dungeon.
            imperial_steel_fragment|&7Imperial Steel Fragment|IRON_NUGGET|DUNGEON_MATERIAL|UNCOMMON|A chipped piece of Imperial steel armor.
            imperial_arrowhead|&7Imperial Arrowhead|FLINT|DUNGEON_MATERIAL|UNCOMMON|A sharp arrowhead used by Imperial archers.
            imperial_scout_badge|&aImperial Scout Badge|EMERALD|DUNGEON_MATERIAL|RARE|A badge worn by Imperial scouts.
            imperial_officer_seal|&eImperial Officer Seal|GOLD_INGOT|DUNGEON_MATERIAL|EPIC|A seal proving the rank of an Imperial officer.
            cracked_monster_core|&8Cracked Monster Core|CHARCOAL|MONSTER_MATERIAL|UNCOMMON|A cracked core taken from a dungeon monster.
            dungeon_hide|&6Dungeon Hide|LEATHER|DUNGEON_MATERIAL|COMMON|Tough hide from a dungeon creature.
            dungeon_fang|&fDungeon Fang|QUARTZ|DUNGEON_MATERIAL|UNCOMMON|A sharp fang from a dungeon beast.
            dungeon_claw|&7Dungeon Claw|PRISMARINE_SHARD|DUNGEON_MATERIAL|UNCOMMON|A claw from a hostile dungeon creature.
            cursed_fabric|&5Cursed Fabric|PURPLE_WOOL|DUNGEON_MATERIAL|RARE|Fabric stained with cursed magic.
            undead_ash|&8Undead Ash|GUNPOWDER|MONSTER_MATERIAL|COMMON|Ash left behind by undead monsters.
            rotten_monster_flesh|&2Rotten Monster Flesh|ROTTEN_FLESH|MONSTER_MATERIAL|COMMON|Rotten flesh from a corrupted monster.
            enchanted_monster_bone|&aEnchanted Monster Bone|BONE|MONSTER_MATERIAL|RARE|A monster bone infused with magic.
            shadow_dust|&5Shadow Dust|GLOWSTONE_DUST|DUNGEON_MATERIAL|RARE|Dust gathered from shadow creatures.
            broken_guard_hilt|&7Broken Guard Hilt|IRON_NUGGET|DUNGEON_MATERIAL|UNCOMMON|A broken sword hilt from a fallen guard.
            monster_soul_shard|&bMonster Soul Shard|AMETHYST_SHARD|MONSTER_MATERIAL|EPIC|A shard containing the soul of a dungeon monster.
            """;

    private static final String RECIPES = """
            ember_fang_sword|WEAPONS|ember_fang|Ember Fang|combat|2|c:ember_fang*16,c:sun_fragment*4,m:STICK*1
            frostbite_longsword|WEAPONS|frost_core|Frost Core|mining|3|c:frost_core*24,c:frozen_bone*16,m:STICK*1
            royal_guard_blade|WEAPONS|royal_guard|Royal Guard|dungeon|2|c:royal_guard_emblem*8,c:enchanted_iron_ingot*32,m:STICK*1
            voidsteel_saber|WEAPONS|voidsteel|Voidsteel|dungeon|4|c:voidsteel_ingot*16,c:void_dust*32,m:STICK*1
            sunforged_broadsword|WEAPONS|sun_fragment|Sun Fragment|combat|3|c:sun_fragment*32,c:enchanted_gold_ingot*16,m:STICK*1
            shadow_sting_dagger|WEAPONS|shadow_cloth|Shadow Cloth|dungeon|2|c:shadow_stinger*8,c:shadow_cloth*16,m:STICK*1
            venom_tip_dagger|WEAPONS|venom_sac|Venom Sac|combat|2|c:venom_sac*16,c:toxic_vial*8,m:STICK*1
            glassfang_dagger|WEAPONS|glass_fang|Glass Fang|mining|2|c:glass_fang*12,c:crystal_fragment*16,m:STICK*1
            crimson_needle|WEAPONS|crimson_needle_fragment|Crimson Needle|combat|3|c:crimson_needle_fragment*24,c:enchanted_redstone*32,m:STICK*1
            nightfall_shiv|WEAPONS|nightfall_cloth|Nightfall Cloth|dungeon|3|c:nightfall_cloth*24,c:shadow_cloth*32,m:STICK*1
            sakura_katana|WEAPONS|sakura_petals|Sakura Petals|foraging|2|c:sakura_petals*32,c:bamboo_fiber*16,m:STICK*1
            stormcut_katana|WEAPONS|storm_essence_magic|Storm Essence|magic|3|c:storm_essence*24,m:LIGHTNING_ROD*1,m:STICK*1
            oni_katana|WEAPONS|oni_horn|Oni Horn|dungeon|4|c:oni_horn*16,c:voidsteel_ingot*8,m:STICK*1
            moonlit_katana|WEAPONS|moon_shard|Moon Shard|magic|3|c:moon_shard*32,c:crystal_fragment*32,m:STICK*1
            bamboo_edge_katana|WEAPONS|bamboo_fiber|Bamboo Fiber|foraging|1|c:bamboo_fiber*32,m:STICK*1
            windpiercer_bow|WEAPONS|wind_feather|Wind Feather|foraging|2|c:wind_feather*16,m:STRING*32,m:STICK*3
            emberstring_bow|WEAPONS|ember_string|Ember String|combat|3|c:ember_string*16,c:ember_fang*8,m:STICK*3
            frozen_hunter_bow|WEAPONS|frozen_hide|Frozen Hide|fishing|3|c:frozen_hide*24,m:STRING*32,m:STICK*3
            royal_crossbow|WEAPONS|royal_bolt_head|Royal Bolt|dungeon|2|c:royal_bolt_head*16,c:enchanted_iron_ingot*24,m:TRIPWIRE_HOOK*1
            voidshot_bow|WEAPONS|void_arrowhead|Void Arrowhead|dungeon|4|c:void_arrowhead*24,c:void_dust*32,m:STICK*3
            spark_wand|WEAPONS|spark_dust|Spark Dust|magic|1|c:spark_dust*16,m:STICK*1
            frost_wand|WEAPONS|frost_dust|Frost Dust|magic|2|c:frost_dust*24,c:frost_core*8,m:STICK*1
            toxic_wand|WEAPONS|toxic_dust|Toxic Dust|magic|2|c:toxic_dust*24,c:toxic_vial*8,m:STICK*1
            void_wand|WEAPONS|void_dust_magic|Void Dust|magic|3|c:void_dust*32,m:END_ROD*1,m:STICK*1
            sunbeam_wand|WEAPONS|sun_dust|Sun Dust|magic|3|c:sun_dust*32,c:sun_fragment*16,m:STICK*1
            apprentice_staff|WEAPONS|apprentice_rune|Apprentice Rune|magic|1|c:apprentice_rune*8,m:STICK*2
            stormcaller_staff|WEAPONS|storm_rune|Storm Rune|magic|3|c:storm_rune*16,c:storm_essence*32,m:LIGHTNING_ROD*1
            necrotic_staff|WEAPONS|necrotic_bone|Necrotic Bone|dungeon|4|c:necrotic_bone*32,c:shadow_cloth*32,m:BONE*1
            crystal_staff|WEAPONS|crystal_focus|Crystal Focus|mining|3|c:crystal_focus*16,c:crystal_fragment*64,m:AMETHYST_SHARD*1
            infernal_staff|WEAPONS|infernal_core|Infernal Core|combat|4|c:infernal_core*8,c:sun_dust*32,m:BLAZE_ROD*1
            lesser_healing_potion|FARMING|healing_herb|Healing Herb|alchemy|1|c:healing_herb*8,m:GLASS_BOTTLE*1
            greater_healing_potion|FARMING|healing_herb|Healing Herb|alchemy|3|c:healing_herb*32,c:crystal_fragment*4,m:GLASS_BOTTLE*1
            lesser_mana_potion|FARMING|mana_leaf|Mana Leaf|alchemy|1|c:mana_leaf*8,m:GLASS_BOTTLE*1
            greater_mana_potion|FARMING|mana_leaf|Mana Leaf|alchemy|3|c:mana_leaf*32,c:crystal_fragment*4,m:GLASS_BOTTLE*1
            iron_skin_potion|FARMING|iron_scale|Iron Scale|alchemy|2|c:iron_scale*16,c:enchanted_iron_ingot*8,m:GLASS_BOTTLE*1
            swift_step_potion|FARMING|swift_leaf|Swift Leaf|alchemy|2|c:swift_leaf*16,m:SUGAR*8,m:GLASS_BOTTLE*1
            honey_bread|FARMING|honey_glaze|Honey Glaze|farming|1|c:honey_glaze*4,m:BREAD*1
            spiced_mushroom_soup|FARMING|spiced_mushroom|Spiced Mushroom|farming|2|c:spiced_mushroom*8,m:MUSHROOM_STEW*1
            fishermans_stew|FISHING|fisherman_spice|Fisherman Spice|fishing|2|c:fisherman_spice*8,m:COD*16,m:BOWL*1
            miners_pie|MINING|miners_crust|Miner's Crust|mining|2|c:miners_crust*8,m:PUMPKIN_PIE*1
            forest_berry_tart|FORAGING|forest_berries|Forest Berries|foraging|2|c:forest_berries*16,m:SWEET_BERRIES*16
            royal_feast_plate|FARMING|royal_plate|Royal Plate|farming|3|c:royal_plate*1,m:COOKED_BEEF*16,c:honey_glaze*8
            miners_luck_charm|ACCESSORIES|luck_fragment|Luck Fragment|mining|2|c:luck_fragment*16,m:RAW_GOLD*16
            farmers_blessing_charm|ACCESSORIES|blessed_seed|Blessed Seed|farming|2|c:blessed_seed*16,m:WHEAT*32
            fishermans_pearl_charm|ACCESSORIES|pearl_scale|Pearl Scale|fishing|3|c:pearl_scale*16,m:ENDER_PEARL*4
            shadow_luck_charm|ACCESSORIES|shadow_luck_shard|Shadow Luck Shard|dungeon|3|c:shadow_luck_shard*16,c:shadow_cloth*32
            warriors_focus_charm|ACCESSORIES|warrior_focus_shard|Warrior Focus Shard|combat|2|c:warrior_focus_shard*16,m:REDSTONE*32
            ancient_guard_charm|ACCESSORIES|ancient_guard_plate|Ancient Guard Plate|dungeon|4|c:ancient_guard_plate*16,c:royal_guard_emblem*16
            totem_of_minor_vigor|ACCESSORIES|vigor_wood|Vigor Wood|accessories|1|c:vigor_wood*16,m:TOTEM_OF_UNDYING*1
            totem_of_mana_flow|ACCESSORIES|mana_thread_accessories|Mana Thread|accessories|2|c:mana_thread*24,c:mana_leaf*16,m:TOTEM_OF_UNDYING*1
            totem_of_battle_fury|ACCESSORIES|battle_fury_shard|Battle Fury Shard|combat|3|c:battle_fury_shard*24,c:warrior_focus_shard*16,m:TOTEM_OF_UNDYING*1
            totem_of_deep_mines|ACCESSORIES|deep_mine_relic|Deep Mine Relic|mining|3|c:deep_mine_relic*16,c:luck_fragment*24,m:TOTEM_OF_UNDYING*1
            totem_of_ancient_light|ACCESSORIES|ancient_light_shard|Ancient Light Shard|dungeon|4|c:ancient_light_shard*24,c:royal_crest*1,m:TOTEM_OF_UNDYING*1
            totem_of_void_resistance|ACCESSORIES|void_resistance_shard|Void Resistance Shard|dungeon|4|c:void_resistance_shard*24,c:void_dust*32,m:TOTEM_OF_UNDYING*1
            bronze_adventurer_necklace|ACCESSORIES|bronze_chain|Bronze Chain|accessories|1|c:bronze_chain*16,m:GOLD_NUGGET*16
            crystal_heart_necklace|ACCESSORIES|crystal_heart|Crystal Heart|accessories|2|c:crystal_heart*16,c:crystal_fragment*16
            hunters_fang_necklace|ACCESSORIES|hunter_fang|Hunter Fang|combat|2|c:hunter_fang*16,m:RABBIT_FOOT*4
            royal_sun_necklace|ACCESSORIES|royal_sun_gem|Royal Sun Gem|accessories|3|c:royal_sun_gem*16,c:sun_fragment*16
            shadow_chain_necklace|ACCESSORIES|shadow_chain_link|Shadow Chain Link|dungeon|3|c:shadow_chain_link*16,c:shadow_cloth*32
            ascended_star_necklace|ACCESSORIES|ascended_star_fragment|Ascended Star Fragment|accessories|4|c:ascended_star_fragment*16,m:NETHER_STAR*1
            miners_ring|ACCESSORIES|deep_mine_relic|Deep Mine Relic|mining|2|c:deep_mine_relic*8,m:RAW_GOLD*16
            farmers_ring|ACCESSORIES|blessed_seed|Blessed Seed|farming|2|c:blessed_seed*8,m:WHEAT*32
            fishermans_ring|ACCESSORIES|pearl_scale|Pearl Scale|fishing|2|c:pearl_scale*8,m:PRISMARINE_SHARD*16
            warriors_ring|ACCESSORIES|warrior_focus_shard|Warrior Focus Shard|combat|2|c:warrior_focus_shard*8,m:IRON_INGOT*16
            mana_ring|ACCESSORIES|mana_thread_magic|Mana Thread|magic|2|c:mana_thread*8,c:mana_leaf*16
            coral_fishing_rod|FISHING|coral|Coral|fishing|2|c:coral_fragment*16,m:STRING*32,m:STICK*3
            deep_sea_rod|FISHING|deep_sea|Deep Sea|fishing|3|c:deep_sea_scale*24,c:pearl_scale*16,m:STICK*3
            angler_charm|FISHING|fisherman_spice|Fisherman Spice|fishing|2|c:fisherman_spice*16,m:COD*32
            sea_creature_totem|FISHING|pearl_scale|Pearl Scale|fishing|4|c:pearl_scale*32,c:deep_sea_scale*32,m:TOTEM_OF_UNDYING*1
            treasure_hook|FISHING|treasure_fragment|Treasure Fragment|fishing|3|c:treasure_fragment*24,m:IRON_INGOT*16
            harvesters_gloves|FARMING|blessed_seed|Blessed Seed|farming|3|c:blessed_seed*32,m:LEATHER*16
            farmers_belt|FARMING|honey_glaze|Honey Glaze|farming|3|c:honey_glaze*32,m:WHEAT*64
            crop_growth_charm|FARMING|swift_leaf|Swift Leaf|farming|3|c:swift_leaf*32,c:blessed_seed*32
            golden_harvest_hoe|FARMING|golden_harvest|Golden Harvest|farming|3|c:golden_harvest_fragment*24,m:GOLDEN_HOE*1
            sugar_cane_cutter|FARMING|sugar_cane|Sugar Cane|farming|3|c:enchanted_sugar_cane*16,c:golden_harvest_fragment*16,m:IRON_HOE*1
            crystal_pickaxe|MINING|crystal_fragment|Crystal Fragment|mining|3|c:crystal_fragment*64,m:STICK*2
            deep_mine_drill|MINING|deep_mine_relic|Deep Mine Relic|mining|4|c:deep_mine_relic*32,c:enchanted_iron_ingot*64,m:REDSTONE_BLOCK*8
            lumber_axe|FORAGING|bamboo_fiber|Bamboo Fiber|foraging|2|c:bamboo_fiber*32,c:enchanted_oak_log*32,m:STICK*2
            storm_shovel|MINING|storm_essence_mining|Storm Essence|mining|2|c:storm_essence*16,m:IRON_SHOVEL*1
            multi_tool_of_the_ancients|UTILITY|ancient_tool_relic|Ancient Tool Relic|tools|5|c:ancient_tool_relic*16,c:crystal_fragment*64,c:voidsteel_ingot*8
            """;

    public static void applyItems(ConfigurationSection root) {
        ConfigurationSection items = section(root, "custom-items.items");
        for (String line : MATERIALS.strip().split("\\R")) {
            String[] p = line.trim().split("\\|", -1);
            if (p.length < 7) continue;
            String id = normalize(p[0]);
            String path = id + ".";
            items.set(path + "name", p[1]);
            items.set(path + "material", p[2]);
            items.set(path + "type", normalizedMaterialType(p[3]));
            items.set(path + "mmo.type", normalizedMaterialType(p[3]));
            items.set(path + "rarity", "UNCOMMON");
            items.set(path + "category", "materials");
            items.set(path + "lore", List.of("&7A stackable custom material.", "&7Used for collection recipe unlocks."));
            items.set(path + "mmo.random-roll", false);
            items.set(path + "mmo.stats", null);
            items.set(path + "ability.type", "none");
            items.set(path + "ability.mana-cost", 0);
            items.set(path + "consumable.enabled", false);
            items.set(path + "unbreakable", false);
            items.set(path + "max-stack-size", 64);
            items.set(path + "sell-price", 5);
            items.set(path + "guild-sellable", true);
            items.set(path + "sellall-allowed", true);
            items.set(path + "collection.drop.activity", p[5]);
            items.set(path + "collection.drop.sources", List.of(p[6].split(",")));
            items.set(path + "collection.drop.chance", 6.0D);
        }
        for(String line:REQUESTED_DUNGEON_MATERIALS.strip().split("\\R")){
            String[] p=line.trim().split("\\|",-1);if(p.length<6)continue;String path=normalize(p[0])+".";
            items.set(path+"name",p[1]);items.set(path+"material",p[2]);items.set(path+"type",p[3]);
            items.set(path+"mmo.type","RECIPE_MATERIAL");items.set(path+"rarity",p[4]);
            items.set(path+"category",p[3].equals("DUNGEON_MATERIAL")?"dungeon_materials":"combat_materials");
            items.set(path+"lore",List.of("&7"+p[5]));items.set(path+"mmo.random-roll",false);items.set(path+"mmo.stats",null);
            items.set(path+"ability.type","none");items.set(path+"ability.mana-cost",0);items.set(path+"consumable.enabled",false);
            items.set(path+"unbreakable",false);items.set(path+"max-stack-size",64);items.set(path+"guild-sellable",true);
            items.set(path+"sellall-allowed",true);items.set(path+"tradeable",true);items.set(path+"soulbound",false);
            items.set(path+"collection.category",p[3].equals("DUNGEON_MATERIAL")?"dungeon":"combat");
            items.set(path+"recipe-material.category",p[3].equals("DUNGEON_MATERIAL")?"dungeon":"combat");
        }
        applyRoyalScrap(items);
        for (String line : EXTRA_OUTPUTS.strip().split("\\R")) {
            String[] p = line.trim().split("\\|", -1);
            if (p.length < 8) continue;
            String path = normalize(p[0]) + ".";
            items.set(path + "name", p[1]);
            items.set(path + "material", p[2]);
            items.set(path + "type", p[3]);
            items.set(path + "mmo.type", p[3]);
            items.set(path + "rarity", p[4]);
            items.set(path + "category", p[5]);
            items.set(path + "lore", List.of("&7" + p[6]));
            items.set(path + "mmo.random-roll", true);
            items.set(path + "mmo.stats", null);
            for (String stat : p[7].split(";")) {
                String[] value = stat.split("=", 2);
                if (value.length == 2) {
                    int amount = integer(value[1], 0);
                    items.set(path + "mmo.stats." + value[0] + ".min", amount);
                    items.set(path + "mmo.stats." + value[0] + ".max", amount);
                }
            }
            items.set(path + "ability.type", "none");
            items.set(path + "ability.mana-cost", 0);
            items.set(path + "consumable.enabled", false);
            items.set(path + "unbreakable", true);
        }
    }

    /**
     * Adventure Guild material registered before its shop rows are created.
     *
     * <p>The old defaults accidentally registered {@code royal_scarp}, while the shop correctly referenced
     * {@code royal_scrap}. Keep this definition explicit so its PDC type, fixed rarity, and material safety
     * flags cannot drift with gear defaults.</p>
     */
    private static void applyRoyalScrap(ConfigurationSection items) {
        String path = "royal_scrap.";
        items.set(path + "name", "&9Royal Scrap");
        items.set(path + "material", "IRON_NUGGET");
        items.set(path + "type", "MATERIAL");
        items.set(path + "mmo.type", "MATERIAL");
        items.set(path + "category", "dungeon_materials");
        items.set(path + "material-category", "DUNGEON_MATERIAL");
        items.set(path + "collection.category", "dungeon");
        items.set(path + "recipe-material.category", "dungeon");
        items.set(path + "rarity", "RARE");
        items.set(path + "fixed-rarity", true);
        items.set(path + "lore", List.of(
                "&7A worn scrap from old royal equipment.",
                "&7Used by the Adventurer's Guild for trades."
        ));
        items.set(path + "max-stack-size", 64);
        items.set(path + "random-stats-allowed", false);
        items.set(path + "random-rarity-allowed", false);
        items.set(path + "random-rarity", false);
        items.set(path + "mmo.random-roll", false);
        items.set(path + "mmo.stats", null);
        items.set(path + "reforge-allowed", false);
        items.set(path + "enchant-allowed", false);
        items.set(path + "special-item", false);
        items.set(path + "developer-notebook", false);
        items.set(path + "preview-item", false);
        items.set(path + "admin-item", false);
        items.set(path + "ability.type", "none");
        items.set(path + "ability.mana-cost", 0);
        items.set(path + "consumable.enabled", false);
        items.set(path + "unbreakable", false);
        items.set(path + "guild-sellable", true);
        items.set(path + "sellall-allowed", true);
        items.set(path + "tradeable", true);
        items.set(path + "soulbound", false);
    }

    public static void applyCollectionsAndRecipes(ConfigurationSection root) {
        categories(root);
        for (String line : RECIPES.strip().split("\\R")) {
            String[] p = line.trim().split("\\|", -1);
            if (p.length < 7) continue;
            String id = normalize(p[0]);
            String collectionId = normalize(p[2]);
            String collectionCategory = normalize(p[4]);
            int tier = integer(p[5], 1);
            String recipe = "recipes." + id;
            root.set(recipe, null);
            root.set(recipe + ".display-name", root.getString("custom-items.items." + id + ".name", "&f" + pretty(id)));
            root.set(recipe + ".category", p[1]);
            root.set(recipe + ".result", id);
            root.set(recipe + ".unlock.type", "COLLECTION");
            root.set(recipe + ".unlock.collection", collectionId);
            root.set(recipe + ".unlock.tier", tier);

            String[] ingredients = p[6].split(",");
            StringBuilder shape = new StringBuilder();
            for (int index = 0; index < ingredients.length && index < 3; index++) {
                char symbol = (char) ('A' + index);
                String[] amount = ingredients[index].split("\\*", 2);
                String value = amount[0];
                boolean custom = value.regionMatches(true, 0, "c:", 0, 2);
                String ingredient = value.substring(2);
                root.set(recipe + ".ingredients." + symbol, (custom ? "custom:" : "material:") + ingredient);
                root.set(recipe + ".amounts." + symbol, Math.clamp(integer(amount.length > 1 ? amount[1] : "1", 1), 1, 64));
                shape.append(symbol);
            }
            while (shape.length() < 3) shape.append(' ');
            root.set(recipe + ".shape", List.of(shape.toString(), "   ", "   "));

            String trackedMaterial = trackedMaterial(ingredients[0]);
            String collection = "collections.items." + collectionId;
            root.set(collection + ".name", "&e" + p[3] + " Collection");
            root.set(collection + ".category", collectionCategory);
            root.set(collection + ".icon", root.getString("custom-items.items." + trackedMaterial + ".material", "CHEST"));
            root.set(collection + ".materials", List.of(trackedMaterial));

            String tierPath = "collections." + collectionCategory + "." + collectionId + ".tiers." + tier;
            root.set(tierPath + ".required", tierAmount(tier));
            List<String> rewards = new ArrayList<>(root.getStringList(tierPath + ".rewards"));
            String reward = "recipe:" + id;
            if (!rewards.contains(reward)) rewards.add(reward);
            root.set(tierPath + ".rewards", rewards);
        }
    }

    private static void categories(ConfigurationSection root) {
        category(root, "combat", "Combat", "IRON_SWORD");
        category(root, "mining", "Mining", "DIAMOND_PICKAXE");
        category(root, "farming", "Farming", "WHEAT");
        category(root, "foraging", "Foraging", "OAK_LOG");
        category(root, "fishing", "Fishing", "FISHING_ROD");
        category(root, "dungeon", "Dungeon", "ECHO_SHARD");
        category(root, "alchemy", "Alchemy", "BREWING_STAND");
        category(root, "magic", "Magic", "BLAZE_ROD");
        category(root, "accessories", "Accessories", "EMERALD");
        category(root, "tools", "Tools", "NETHERITE_PICKAXE");
    }

    private static void category(ConfigurationSection root, String id, String name, String icon) {
        root.set("collections.categories." + id + ".name", name + " Collection");
        root.set("collections.categories." + id + ".icon", icon);
    }

    private static String normalizedMaterialType(String requested) {
        String type = requested.toUpperCase(Locale.ROOT);
        return switch (type) {
            case "DUNGEON_MATERIAL", "FARMING_MATERIAL", "FISHING_MATERIAL", "MINING_MATERIAL", "FORAGING_MATERIAL", "MONSTER_MATERIAL" -> "RECIPE_MATERIAL";
            default -> type;
        };
    }

    private static String trackedMaterial(String ingredient) {
        String value = ingredient.split("\\*", 2)[0];
        return normalize(value.length() > 2 ? value.substring(2) : value);
    }

    private static int tierAmount(int tier) {
        return switch (tier) {
            case 1 -> 50;
            case 2 -> 150;
            case 3 -> 400;
            case 4 -> 1_000;
            default -> 2_500;
        };
    }

    private static ConfigurationSection section(ConfigurationSection root, String path) {
        ConfigurationSection section = root.getConfigurationSection(path);
        return section == null ? root.createSection(path) : section;
    }

    private static int integer(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_').trim();
    }

    private static String pretty(String value) {
        StringBuilder result = new StringBuilder();
        for (String word : normalize(value).split("_")) {
            if (word.isBlank()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }
}
