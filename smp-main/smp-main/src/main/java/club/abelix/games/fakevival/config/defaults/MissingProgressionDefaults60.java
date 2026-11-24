package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Guarded Java-only defaults for the sixty vanilla-path progression recipes. */
public final class MissingProgressionDefaults60 {
    private MissingProgressionDefaults60() {}

    // id|base material|material subcategory
    private static final String MATERIALS = """
stonebound_core|STONE|MINING_MATERIAL
deepslate_plate|DEEPSLATE_TILES|MINING_MATERIAL
granite_powder|GRANITE|MINING_MATERIAL
diorite_powder|DIORITE|MINING_MATERIAL
andesite_powder|ANDESITE|MINING_MATERIAL
tuff_dust|TUFF|MINING_MATERIAL
calcite_shard|CALCITE|MINING_MATERIAL
dripstone_needle|POINTED_DRIPSTONE|MINING_MATERIAL
sandstone_core|SANDSTONE|MINING_MATERIAL
gravel_flint_mix|FLINT|MINING_MATERIAL
clay_binding_mold|CLAY_BALL|MINING_MATERIAL
terracotta_guard_tile|TERRACOTTA|MINING_MATERIAL
ice_crystal|PACKED_ICE|MINING_MATERIAL
snowpacked_cloth|WHITE_WOOL|FORAGING_MATERIAL
muddy_root|MUDDY_MANGROVE_ROOTS|FORAGING_MATERIAL
mossy_thread|MOSS_BLOCK|FORAGING_MATERIAL
vine_rope_bundle|VINE|FORAGING_MATERIAL
leafy_fiber|OAK_LEAVES|FORAGING_MATERIAL
sapling_growth_core|OAK_SAPLING|FORAGING_MATERIAL
flower_essence|OXEYE_DAISY|FORAGING_MATERIAL
dye_magic_dust|MAGENTA_DYE|MAGIC_MATERIAL
copper_spring|COPPER_INGOT|MINING_MATERIAL
coal_heat_core|COAL_BLOCK|MINING_MATERIAL
quartz_mirror|QUARTZ|MAGIC_MATERIAL
amethyst_pulse_core|AMETHYST_SHARD|MAGIC_MATERIAL
blackstone_spike|BLACKSTONE|MINING_MATERIAL
basalt_weight_core|BASALT|MINING_MATERIAL
soul_sand_essence|SOUL_SAND|MAGIC_MATERIAL
nether_wart_powder|NETHER_WART|FARMING_MATERIAL
chorus_warp_thread|CHORUS_FRUIT|MAGIC_MATERIAL
purpur_rune_plate|PURPUR_BLOCK|MAGIC_MATERIAL
end_stone_fragment|END_STONE|MAGIC_MATERIAL
egg_yolk_mix|EGG|FARMING_MATERIAL
milk_cream_core|MILK_BUCKET|FARMING_MATERIAL
honey_sticky_core|HONEYCOMB|FARMING_MATERIAL
cocoa_dark_powder|COCOA_BEANS|FARMING_MATERIAL
cactus_spike_core|CACTUS|FARMING_MATERIAL
pumpkin_seed_core|PUMPKIN_SEEDS|FARMING_MATERIAL
melon_sweet_slice|MELON_SLICE|FARMING_MATERIAL
glow_berry_core|GLOW_BERRIES|FARMING_MATERIAL
mushroom_spore_mix|BROWN_MUSHROOM|FARMING_MATERIAL
gunpowder_spark_core|GUNPOWDER|MONSTER_MATERIAL
slime_stretch_gel|SLIME_BALL|MONSTER_MATERIAL
magma_heat_scale|MAGMA_CREAM|MONSTER_MATERIAL
blaze_rod_shard|BLAZE_ROD|MONSTER_MATERIAL
ender_thread|ENDER_PEARL|MAGIC_MATERIAL
ghast_wail_core|GHAST_TEAR|MONSTER_MATERIAL
phantom_shadow_cloth|PHANTOM_MEMBRANE|MONSTER_MATERIAL
rabbit_swiftness_foot|RABBIT_FOOT|MONSTER_MATERIAL
prismarine_scale_core|PRISMARINE_SHARD|FISHING_MATERIAL
prismarine_crystal_lens|PRISMARINE_CRYSTALS|FISHING_MATERIAL
sea_lantern_orb|SEA_LANTERN|FISHING_MATERIAL
nautilus_shell_shard|NAUTILUS_SHELL|FISHING_MATERIAL
kelp_wrap_core|KELP|FISHING_MATERIAL
cod_fin_bundle|COD|FISHING_MATERIAL
salmon_fin_bundle|SALMON|FISHING_MATERIAL
puffer_poison_spike|PUFFERFISH|FISHING_MATERIAL
tropical_color_fin|TROPICAL_FISH|FISHING_MATERIAL
""";

    // Exact requested ingredient IDs which were not present in older registries.
    private static final String SUPPORT_MATERIALS = """
flower_magic_pollen|YELLOW_DYE|MAGIC_MATERIAL
dye_color_essence|MAGENTA_DYE|MAGIC_MATERIAL
copper_tech_wire|COPPER_INGOT|MINING_MATERIAL
blackstone_armor_plate|POLISHED_BLACKSTONE|MINING_MATERIAL
melon_sweet_core|MELON|FARMING_MATERIAL
cod_ocean_scale|COD|FISHING_MATERIAL
salmon_red_scale|SALMON|FISHING_MATERIAL
tropical_color_scale|TROPICAL_FISH|FISHING_MATERIAL
sea_lantern_shine|SEA_LANTERN|FISHING_MATERIAL
jungle_spirit_vine|VINE|FORAGING_MATERIAL
""";

    // id|category|icon|tracked vanilla materials (comma separated)|rare
    private static final String COLLECTIONS = """
stone|mining|STONE|STONE|false
deepslate|mining|DEEPSLATE|DEEPSLATE,COBBLED_DEEPSLATE|false
granite|mining|GRANITE|GRANITE|false
diorite|mining|DIORITE|DIORITE|false
andesite|mining|ANDESITE|ANDESITE|false
tuff|mining|TUFF|TUFF|false
calcite|mining|CALCITE|CALCITE|false
dripstone|mining|POINTED_DRIPSTONE|POINTED_DRIPSTONE,DRIPSTONE_BLOCK|false
sand|mining|SAND|SAND,RED_SAND|false
gravel|mining|GRAVEL|GRAVEL|false
clay|mining|CLAY_BALL|CLAY_BALL,CLAY|false
terracotta|mining|TERRACOTTA|TERRACOTTA|false
ice|mining|ICE|ICE,PACKED_ICE,BLUE_ICE|false
snow|mining|SNOWBALL|SNOWBALL,SNOW_BLOCK,POWDER_SNOW_BUCKET|false
mud|foraging|MUD|MUD,MUDDY_MANGROVE_ROOTS|false
moss|foraging|MOSS_BLOCK|MOSS_BLOCK,MOSS_CARPET|false
vine|foraging|VINE|VINE|false
leaves|foraging|OAK_LEAVES|OAK_LEAVES,SPRUCE_LEAVES,BIRCH_LEAVES,JUNGLE_LEAVES,ACACIA_LEAVES,DARK_OAK_LEAVES,MANGROVE_LEAVES,CHERRY_LEAVES|false
sapling|foraging|OAK_SAPLING|OAK_SAPLING,SPRUCE_SAPLING,BIRCH_SAPLING,JUNGLE_SAPLING,ACACIA_SAPLING,DARK_OAK_SAPLING,CHERRY_SAPLING,MANGROVE_PROPAGULE|false
flower|foraging|POPPY|POPPY,DANDELION,OXEYE_DAISY,CORNFLOWER,ALLIUM,AZURE_BLUET,BLUE_ORCHID|false
dye|farming|MAGENTA_DYE|WHITE_DYE,ORANGE_DYE,MAGENTA_DYE,LIGHT_BLUE_DYE,YELLOW_DYE,LIME_DYE,PINK_DYE,GRAY_DYE,LIGHT_GRAY_DYE,CYAN_DYE,PURPLE_DYE,BLUE_DYE,BROWN_DYE,GREEN_DYE,RED_DYE,BLACK_DYE|false
copper|mining|COPPER_INGOT|COPPER_INGOT,RAW_COPPER,COPPER_ORE,DEEPSLATE_COPPER_ORE|false
coal|mining|COAL|COAL,CHARCOAL,COAL_ORE,DEEPSLATE_COAL_ORE|false
quartz|mining|QUARTZ|QUARTZ,NETHER_QUARTZ_ORE|false
amethyst|mining|AMETHYST_SHARD|AMETHYST_SHARD,AMETHYST_CLUSTER|false
blackstone|mining|BLACKSTONE|BLACKSTONE|false
basalt|mining|BASALT|BASALT,SMOOTH_BASALT|false
soul_sand|mining|SOUL_SAND|SOUL_SAND|false
nether_wart|farming|NETHER_WART|NETHER_WART|false
chorus_fruit|farming|CHORUS_FRUIT|CHORUS_FRUIT|false
purpur|mining|PURPUR_BLOCK|PURPUR_BLOCK|false
end_stone|mining|END_STONE|END_STONE|false
egg|farming|EGG|EGG|false
milk|farming|MILK_BUCKET|MILK_BUCKET|false
honey|farming|HONEY_BOTTLE|HONEY_BOTTLE,HONEYCOMB|false
cocoa_beans|farming|COCOA_BEANS|COCOA_BEANS|false
cactus|farming|CACTUS|CACTUS|false
pumpkin|farming|PUMPKIN|PUMPKIN|false
melon|farming|MELON_SLICE|MELON_SLICE|false
glow_berries|farming|GLOW_BERRIES|GLOW_BERRIES|false
mushroom|farming|BROWN_MUSHROOM|BROWN_MUSHROOM,RED_MUSHROOM|false
gunpowder|combat|GUNPOWDER|GUNPOWDER|false
slimeball|combat|SLIME_BALL|SLIME_BALL|false
magma_cream|combat|MAGMA_CREAM|MAGMA_CREAM|false
blaze_rod|combat|BLAZE_ROD|BLAZE_ROD|true
ender_pearl|combat|ENDER_PEARL|ENDER_PEARL|false
ghast_tear|combat|GHAST_TEAR|GHAST_TEAR|true
phantom_membrane|combat|PHANTOM_MEMBRANE|PHANTOM_MEMBRANE|true
rabbit_foot|combat|RABBIT_FOOT|RABBIT_FOOT|true
prismarine_shard|fishing|PRISMARINE_SHARD|PRISMARINE_SHARD|false
prismarine_crystals|fishing|PRISMARINE_CRYSTALS|PRISMARINE_CRYSTALS|false
sea_lantern|fishing|SEA_LANTERN|SEA_LANTERN|false
nautilus_shell|fishing|NAUTILUS_SHELL|NAUTILUS_SHELL|true
kelp|fishing|KELP|KELP|false
cod|fishing|COD|COD|false
salmon|fishing|SALMON|SALMON|false
pufferfish|fishing|PUFFERFISH|PUFFERFISH|false
tropical_fish|fishing|TROPICAL_FISH|TROPICAL_FISH|false
fishing|fishing|FISHING_ROD|COD,SALMON,PUFFERFISH,TROPICAL_FISH|false
""";

    // id|name|base|type|collection|tier|ingredients
    private static final String RECIPES = """
stonebound_pickaxe|Stonebound Pickaxe|DIAMOND_PICKAXE|PICKAXE|stone|4|c:stonebound_core*24,m:STICK*2
deepslate_guard_chestplate|Deepslate Guard Chestplate|IRON_CHESTPLATE|ARMOR|deepslate|5|c:deepslate_plate*32,c:reinforced_hide*16
granite_edge_sword|Granite Edge Sword|IRON_SWORD|SWORD|granite|3|c:granite_powder*24,c:flint_edge*8,m:STICK*1
diorite_light_wand|Diorite Light Wand|BLAZE_ROD|WAND|diorite|3|c:diorite_powder*24,c:quartz_splinter*8,m:STICK*1
andesite_guard_boots|Andesite Guard Boots|IRON_BOOTS|ARMOR|andesite|3|c:andesite_powder*24,c:reinforced_leather*12
tuff_mining_charm|Tuff Mining Charm|TUFF|CHARM|tuff|3|c:tuff_dust*32,c:coal_fiber*16
calcite_focus_ring|Calcite Focus Ring|CALCITE|RING|calcite|3|c:calcite_shard*24,c:quartz_mirror*4
dripstone_dagger|Dripstone Dagger|IRON_SWORD|DAGGER|dripstone|3|c:dripstone_needle*32,m:STICK*1
sandstone_staff|Sandstone Staff|BLAZE_ROD|STAFF|sand|4|c:sandstone_core*32,c:sand_glass_core*8,m:BLAZE_ROD*1
gravel_flint_bow|Gravel Flint Bow|BOW|BOW|gravel|3|c:gravel_flint_mix*32,c:hardened_string*24,m:STICK*3
clay_mold_totem|Clay Mold Totem|TOTEM_OF_UNDYING|TOTEM|clay|3|c:clay_binding_mold*24,m:TOTEM_OF_UNDYING*1
terracotta_guard_helmet|Terracotta Guard Helmet|IRON_HELMET|ARMOR|terracotta|4|c:terracotta_guard_tile*32,c:reinforced_hide*12
ice_crystal_wand|Ice Crystal Wand|BLAZE_ROD|WAND|ice|4|c:ice_crystal*32,c:frosted_scale*8,m:STICK*1
snowpacked_boots|Snowpacked Boots|LEATHER_BOOTS|ARMOR|snow|3|c:snowpacked_cloth*24,c:reinforced_leather*12
muddy_root_charm|Muddy Root Charm|MUDDY_MANGROVE_ROOTS|CHARM|mud|3|c:muddy_root*32,c:mossy_thread*12
mossy_thread_necklace|Mossy Thread Necklace|MOSS_BLOCK|NECKLACE|moss|3|c:mossy_thread*32,m:GOLD_NUGGET*16
vine_rope_whip|Vine Rope Whip|IRON_SWORD|SWORD|vine|4|c:vine_rope_bundle*32,c:jungle_spirit_vine*8,m:STICK*1
leafy_fiber_leggings|Leafy Fiber Leggings|LEATHER_LEGGINGS|ARMOR|leaves|3|c:leafy_fiber*32,c:reinforced_leather*16
sapling_growth_totem|Sapling Growth Totem|TOTEM_OF_UNDYING|TOTEM|sapling|3|c:sapling_growth_core*24,m:TOTEM_OF_UNDYING*1
flower_essence_ring|Flower Essence Ring|POPPY|RING|flower|3|c:flower_essence*32,c:flower_magic_pollen*8
dye_magic_charm|Dye Magic Charm|MAGENTA_DYE|CHARM|dye|3|c:dye_magic_dust*32,c:dye_color_essence*8
copper_spring_boots|Copper Spring Boots|IRON_BOOTS|ARMOR|copper|4|c:copper_spring*32,c:copper_tech_wire*16
coal_heat_helmet|Coal Heat Helmet|IRON_HELMET|ARMOR|coal|4|c:coal_heat_core*32,c:coal_forged_plate*16
quartz_mirror_staff|Quartz Mirror Staff|BLAZE_ROD|STAFF|quartz|5|c:quartz_mirror*16,c:quartz_focus_lens*8,m:BLAZE_ROD*1
amethyst_pulse_katana|Amethyst Pulse Katana|DIAMOND_SWORD|KATANA|amethyst|5|c:amethyst_pulse_core*24,c:amethyst_blade_core*16,m:STICK*1
blackstone_spike_sword|Blackstone Spike Sword|IRON_SWORD|SWORD|blackstone|4|c:blackstone_spike*32,c:blackstone_armor_plate*12,m:STICK*1
basalt_weight_chestplate|Basalt Weight Chestplate|IRON_CHESTPLATE|ARMOR|basalt|4|c:basalt_weight_core*32,c:heavy_iron_plate*16
soul_sand_essence_charm|Soul Sand Essence Charm|SOUL_SAND|CHARM|soul_sand|4|c:soul_sand_essence*24,c:soul_spark*8
nether_wart_spice_potion|Nether Wart Spice Potion|POTION|POTION|nether_wart|4|c:nether_wart_powder*32,m:GLASS_BOTTLE*1
chorus_warp_staff|Chorus Warp Staff|END_ROD|STAFF|chorus_fruit|4|c:chorus_warp_thread*24,c:chorus_essence*8,m:END_ROD*1
purpur_rune_ring|Purpur Rune Ring|PURPUR_BLOCK|RING|purpur|3|c:purpur_rune_plate*24,c:chorus_thread*8
end_stone_fragment_pickaxe|End Stone Fragment Pickaxe|DIAMOND_PICKAXE|PICKAXE|end_stone|5|c:end_stone_fragment*32,c:end_stone_core*16,m:STICK*2
egg_yolk_breakfast|Egg Yolk Breakfast|COOKED_CHICKEN|FOOD|egg|3|c:egg_yolk_mix*16,c:campfire_toast*1
milk_cream_drink_plus|Milk Cream Drink Plus|HONEY_BOTTLE|FOOD|milk|3|c:milk_cream_core*16,c:sweet_cream_drink*1
honey_sticky_bread|Honey Sticky Bread|BREAD|FOOD|honey|4|c:honey_sticky_core*16,c:honey_glazed_bread*1
cocoa_dark_cake|Cocoa Dark Cake|CAKE|FOOD|cocoa_beans|4|c:cocoa_dark_powder*24,c:chocolate_cake_slice*1
cactus_spike_dagger_plus|Cactus Spike Dagger Plus|IRON_SWORD|DAGGER|cactus|4|c:cactus_spike_core*24,c:cactus_spine_bundle*16,m:STICK*1
pumpkin_seed_totem|Pumpkin Seed Totem|TOTEM_OF_UNDYING|TOTEM|pumpkin|4|c:pumpkin_seed_core*24,m:TOTEM_OF_UNDYING*1
melon_sweet_ring|Melon Sweet Ring|MELON_SLICE|RING|melon|4|c:melon_sweet_slice*24,c:melon_sweet_core*8
glow_berry_mana_charm|Glow Berry Mana Charm|GLOW_BERRIES|CHARM|glow_berries|4|c:glow_berry_core*24,c:mana_syrup*1
mushroom_spore_stew|Mushroom Spore Stew|MUSHROOM_STEW|FOOD|mushroom|4|c:mushroom_spore_mix*24,c:mushroom_stew_custom*1
gunpowder_spark_wand|Gunpowder Spark Wand|BLAZE_ROD|WAND|gunpowder|4|c:gunpowder_spark_core*24,c:redstone_spark*12,m:STICK*1
slime_stretch_boots|Slime Stretch Boots|LEATHER_BOOTS|ARMOR|slimeball|4|c:slime_stretch_gel*32,c:slime_core*12
magma_heat_sword|Magma Heat Sword|DIAMOND_SWORD|SWORD|magma_cream|5|c:magma_heat_scale*32,c:magma_core*16,m:STICK*1
blaze_rod_shard_staff|Blaze Rod Shard Staff|BLAZE_ROD|STAFF|blaze_rod|5|c:blaze_rod_shard*24,c:blazing_powder_core*12,m:BLAZE_ROD*1
ender_thread_necklace|Ender Thread Necklace|ENDER_PEARL|NECKLACE|ender_pearl|5|c:ender_thread*24,c:void_pearl_fragment*16
ghast_wail_wand|Ghast Wail Wand|BLAZE_ROD|WAND|ghast_tear|4|c:ghast_wail_core*16,c:ghast_soul_thread*8,m:STICK*1
phantom_shadow_boots|Phantom Shadow Boots|LEATHER_BOOTS|ARMOR|phantom_membrane|4|c:phantom_shadow_cloth*24,c:phantom_wing*12
rabbit_swiftness_charm|Rabbit Swiftness Charm|RABBIT_FOOT|CHARM|rabbit_foot|4|c:rabbit_swiftness_foot*16,c:rabbit_luck_foot*8
prismarine_scale_leggings|Prismarine Scale Leggings|DIAMOND_LEGGINGS|ARMOR|prismarine_shard|5|c:prismarine_scale_core*32,c:prismarine_guard_scale*16
prismarine_crystal_wand|Prismarine Crystal Wand|BLAZE_ROD|WAND|prismarine_crystals|5|c:prismarine_crystal_lens*24,c:prismarine_spell_core*12,m:STICK*1
sea_lantern_orb_ring|Sea Lantern Orb Ring|SEA_LANTERN|RING|sea_lantern|4|c:sea_lantern_orb*16,c:sea_lantern_shine*8
nautilus_shell_charm|Nautilus Shell Charm|NAUTILUS_SHELL|CHARM|nautilus_shell|5|c:nautilus_shell_shard*16,c:nautilus_core*8
kelp_wrap_armor|Kelp Wrap Armor|LEATHER_CHESTPLATE|ARMOR|kelp|4|c:kelp_wrap_core*32,c:reinforced_hide*16
cod_fin_boots|Cod Fin Boots|LEATHER_BOOTS|ARMOR|cod|4|c:cod_fin_bundle*32,c:cod_ocean_scale*8
salmon_fin_helmet|Salmon Fin Helmet|LEATHER_HELMET|ARMOR|salmon|4|c:salmon_fin_bundle*32,c:salmon_red_scale*8
puffer_poison_dagger|Puffer Poison Dagger|IRON_SWORD|DAGGER|pufferfish|4|c:puffer_poison_spike*24,c:pufferfish_spice*8,m:STICK*1
tropical_color_necklace|Tropical Color Necklace|TROPICAL_FISH|NECKLACE|tropical_fish|4|c:tropical_color_fin*24,c:tropical_color_scale*8
fishing_master_charm|Fishing Master Charm|HEART_OF_THE_SEA|CHARM|fishing|7|c:cod_fin_bundle*12,c:salmon_fin_bundle*12,c:tropical_color_fin*12,c:storm_pearl*4
sea_master_fishing_rod|Sea Master Fishing Rod|FISHING_ROD|FISHING_ROD|fishing|8|c:kelp_wrap_core*32,c:prismarine_crystal_lens*16,c:storm_pearl*8,m:STICK*3
""";

    private static final int[] COMMON = {50,100,250,500,1000,2500,5000,10000,25000,50000};
    private static final int[] RARE = {10,25,50,100,250,500,1000,2500,5000,10000};

    public static void apply(ConfigurationSection root) {
        applyMaterials(root, MATERIALS);
        applyMaterials(root, SUPPORT_MATERIALS);
        applyCollections(root);
        for (String line : RECIPES.strip().split("\\R")) applyRecipe(root, line);
    }

    public static Set<String> recipeIds() { return ids(RECIPES); }
    public static Set<String> materialIds() { return ids(MATERIALS); }
    public static Set<String> collectionIds() { return ids(COLLECTIONS); }

    private static void applyMaterials(ConfigurationSection root, String dataset) {
        for (String line : dataset.strip().split("\\R")) {
            String[] p = line.split("\\|");
            if (p.length < 3) continue;
            String id = norm(p[0]), base = "custom-items.items." + id;
            if (root.isConfigurationSection(base)) continue;
            root.set(base + ".name", "&f" + pretty(id));
            root.set(base + ".material", p[1]);
            root.set(base + ".type", "RECIPE_MATERIAL");
            root.set(base + ".mmo.type", "RECIPE_MATERIAL");
            root.set(base + ".source-type", p[2]);
            root.set(base + ".category", RequestedMaterialDefaults80.browserCategory(p[2]));
            root.set(base + ".collection.category", subcategory(p[2]));
            root.set(base + ".recipe-material.category", subcategory(p[2]));
            root.set(base + ".rarity", "COMMON");
            root.set(base + ".random-rarity", false);
            root.set(base + ".lore", List.of("&7A stackable progression material."));
            root.set(base + ".max-stack-size", 64);
            root.set(base + ".mmo.random-roll", false);
            root.set(base + ".mmo.stats", null);
            root.set(base + ".ability.type", "none");
            root.set(base + ".ability.mana-cost", 0);
            root.set(base + ".consumable.enabled", false);
            root.set(base + ".unbreakable", false);
            root.set(base + ".tradeable", true);
            root.set(base + ".soulbound", false);
            root.set(base + ".guild-sellable", true);
            root.set(base + ".sellall-allowed", true);
        }
    }

    private static void applyCollections(ConfigurationSection root) {
        for (String line : COLLECTIONS.strip().split("\\R")) {
            String[] p = line.split("\\|", -1);
            if (p.length < 5) continue;
            String id = norm(p[0]), base = "collections.items." + id;
            if (!root.isConfigurationSection(base)) {
                root.set(base + ".name", "&e" + pretty(id) + " Collection");
                root.set(base + ".category", p[1]);
                root.set(base + ".icon", p[2]);
                root.set(base + ".materials", List.of(p[3].split(",")));
            } else {
                if (!root.contains(base + ".category")) root.set(base + ".category", p[1]);
                if (!root.contains(base + ".icon")) root.set(base + ".icon", p[2]);
                List<String> tracked = new ArrayList<>(root.getStringList(base + ".materials"));
                for (String material : p[3].split(",")) if (tracked.stream().noneMatch(material::equalsIgnoreCase)) tracked.add(material);
                root.set(base + ".materials", tracked);
            }
            int[] amounts = Boolean.parseBoolean(p[4]) ? RARE : COMMON;
            for (int tier = 1; tier <= 10; tier++) {
                String tierPath = "collections." + p[1] + "." + id + ".tiers." + tier;
                if (!root.contains(tierPath + ".required")) root.set(tierPath + ".required", amounts[tier - 1]);
                if (!root.contains(tierPath + ".rewards") || root.getStringList(tierPath + ".rewards").isEmpty())
                    root.set(tierPath + ".rewards", List.of("perk:tier_" + tier));
            }
        }
    }

    private static void applyRecipe(ConfigurationSection root, String line) {
        String[] p = line.split("\\|", 7);
        if (p.length < 7) return;
        String id = norm(p[0]), item = "custom-items.items." + id, recipe = "recipes." + id;
        if (!root.isConfigurationSection(item)) {
            root.set(item + ".name", "&a" + p[1]);
            root.set(item + ".material", p[2]);
            root.set(item + ".type", p[3]);
            root.set(item + ".mmo.type", p[3]);
            root.set(item + ".category", outputCategory(p[3]));
            root.set(item + ".rarity", "RARE");
            root.set(item + ".random-rarity", false);
            root.set(item + ".mmo.random-roll", false);
            root.set(item + ".ability.type", "none");
            root.set(item + ".ability.mana-cost", 0);
            root.set(item + ".unbreakable", !Set.of("FOOD", "POTION").contains(p[3]));
            applyConsumable(root, item, id);
        }
        if (!root.isConfigurationSection(recipe)) {
            root.set(recipe + ".display-name", "&a" + p[1]);
            root.set(recipe + ".category", recipeCategory(p[3]));
            root.set(recipe + ".result", id);
            root.set(recipe + ".unlock.type", "COLLECTION");
            root.set(recipe + ".unlock.collection", norm(p[4]));
            root.set(recipe + ".unlock.tier", Integer.parseInt(p[5]));
            StringBuilder shape = new StringBuilder();
            String[] ingredients = p[6].split(",");
            for (int i = 0; i < ingredients.length && i < 4; i++) {
                char symbol = (char) ('A' + i);
                String[] amount = ingredients[i].split("\\*", 2);
                String value = amount[0];
                root.set(recipe + ".ingredients." + symbol, (value.startsWith("c:") ? "custom:" : "material:") + value.substring(2));
                root.set(recipe + ".amounts." + symbol, Math.clamp(Integer.parseInt(amount[1]), 1, 64));
                shape.append(symbol);
            }
            // The book/manual crafter supports four ingredient identities; Bukkit shape remains at most 3 wide.
            String first = shape.substring(0, Math.min(3, shape.length()));
            while (first.length() < 3) first += " ";
            String second = shape.length() > 3 ? shape.substring(3, 4) + "  " : "   ";
            root.set(recipe + ".shape", List.of(first, second, "   "));
        }
        int tier = Integer.parseInt(p[5]);
        String collection = norm(p[4]);
        String category = collectionCategory(collection);
        String tierPath = "collections." + category + "." + collection + ".tiers." + tier;
        List<String> rewards = new ArrayList<>(root.getStringList(tierPath + ".rewards"));
        String reward = "recipe:" + id;
        if (!rewards.contains(reward)) rewards.add(reward);
        root.set(tierPath + ".rewards", rewards);
    }

    private static void applyConsumable(ConfigurationSection root, String item, String id) {
        int heal = switch (id) {
            case "egg_yolk_breakfast" -> 90;
            case "milk_cream_drink_plus" -> 80;
            case "honey_sticky_bread" -> 120;
            case "cocoa_dark_cake" -> 140;
            case "mushroom_spore_stew" -> 130;
            default -> 0;
        };
        boolean potion = id.equals("nether_wart_spice_potion");
        root.set(item + ".consumable.enabled", heal > 0 || potion);
        if (heal > 0) root.set(item + ".consumable.heal", heal);
        if (potion) {
            root.set(item + ".consumable.effects.REGENERATION.amplifier", 0);
            root.set(item + ".consumable.effects.REGENERATION.duration-seconds", 60);
        }
    }

    private static String collectionCategory(String collection) {
        for (String line : COLLECTIONS.strip().split("\\R")) {
            String[] p = line.split("\\|", 3);
            if (norm(p[0]).equals(collection)) return p[1];
        }
        return "mining";
    }

    private static String subcategory(String type) {
        return switch (type) {
            case "FARMING_MATERIAL" -> "farming";
            case "FORAGING_MATERIAL" -> "foraging";
            case "MONSTER_MATERIAL" -> "combat";
            case "FISHING_MATERIAL" -> "fishing";
            case "MAGIC_MATERIAL" -> "magic";
            case "DUNGEON_MATERIAL" -> "dungeon";
            default -> "mining";
        };
    }

    private static String outputCategory(String type) {
        return switch (type) {
            case "ARMOR" -> "armor";
            case "PICKAXE" -> "tools";
            case "FOOD" -> "food";
            case "POTION" -> "potions";
            case "FISHING_ROD" -> "fishing_equipment";
            case "CHARM", "TOTEM", "NECKLACE", "RING" -> "accessories";
            case "WAND", "STAFF" -> "magic";
            default -> "weapons";
        };
    }

    private static String recipeCategory(String type) {
        return switch (type) {
            case "ARMOR" -> "ARMOR";
            case "PICKAXE" -> "TOOLS";
            case "FOOD" -> "FOOD";
            case "POTION" -> "POTIONS";
            case "FISHING_ROD" -> "FISHING_EQUIPMENT";
            case "CHARM", "TOTEM", "NECKLACE", "RING" -> "ACCESSORIES";
            case "WAND", "STAFF" -> "MAGIC";
            default -> "WEAPONS";
        };
    }

    private static Set<String> ids(String dataset) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String line : dataset.strip().split("\\R")) result.add(norm(line.split("\\|", 2)[0]));
        return Set.copyOf(result);
    }

    private static String norm(String value) { return value.toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_'); }
    private static String pretty(String value) {
        StringBuilder out = new StringBuilder();
        for (String part : norm(value).split("_")) {
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }
}
