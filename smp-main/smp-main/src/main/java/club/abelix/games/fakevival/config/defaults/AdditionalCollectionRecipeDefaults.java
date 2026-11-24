package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;
import java.util.Set;

/** Second guarded batch of sixty vanilla-path collection recipes. */
final class AdditionalCollectionRecipeDefaults {
    private AdditionalCollectionRecipeDefaults() {}
    private static final String MATERIALS="""
moonlit_silk|LIGHT_BLUE_WOOL|MAGIC_MATERIAL
reinforced_hide|LEATHER|RECIPE_MATERIAL
compressed_stone_plate|STONE_BRICKS|MINING_MATERIAL
gleaming_iron_core|IRON_INGOT|MINING_MATERIAL
golden_thread|YELLOW_WOOL|RECIPE_MATERIAL
diamond_shard_cluster|DIAMOND|MINING_MATERIAL
emerald_inlay|EMERALD|MINING_MATERIAL
lapis_spell_thread|BLUE_WOOL|MAGIC_MATERIAL
redstone_circuit_core|REDSTONE_TORCH|MINING_MATERIAL
quartz_prism|QUARTZ|MAGIC_MATERIAL
amethyst_blade_core|AMETHYST_SHARD|MAGIC_MATERIAL
ancient_log_handle|STICK|FORAGING_MATERIAL
mossy_fiber|MOSS_BLOCK|FORAGING_MATERIAL
frosted_leaf|AZALEA_LEAVES|FORAGING_MATERIAL
jungle_vine_rope|VINE|FORAGING_MATERIAL
mangrove_resin|MANGROVE_ROOTS|FORAGING_MATERIAL
cherry_petals_bundle|PINK_PETALS|FORAGING_MATERIAL
wheat_charm_core|WHEAT|FARMING_MATERIAL
sugar_crystal|SUGAR|FARMING_MATERIAL
melon_juice_core|MELON_SLICE|FARMING_MATERIAL
pumpkin_shell_plate|PUMPKIN|FARMING_MATERIAL
beetroot_life_extract|BEETROOT|FARMING_MATERIAL
carrot_spirit|CARROT|FARMING_MATERIAL
potato_guard_starch|POTATO|FARMING_MATERIAL
bone_guard_plate|BONE_BLOCK|MONSTER_MATERIAL
string_tension_core|STRING|MONSTER_MATERIAL
spider_venom_glass|LIME_STAINED_GLASS|MONSTER_MATERIAL
ender_eye_fragment|ENDER_EYE|MAGIC_MATERIAL
slime_binding_gel|SLIME_BALL|MONSTER_MATERIAL
magma_fang|MAGMA_CREAM|MONSTER_MATERIAL
ghast_soul_thread|WHITE_WOOL|MONSTER_MATERIAL
phantom_cloth|PHANTOM_MEMBRANE|MONSTER_MATERIAL
guardian_scale_plate|PRISMARINE|FISHING_MATERIAL
prismarine_spell_core|PRISMARINE_CRYSTALS|MAGIC_MATERIAL
kelp_rope|KELP|FISHING_MATERIAL
cod_oil|COD|FISHING_MATERIAL
salmon_scale|SALMON|FISHING_MATERIAL
puffer_spike|PUFFERFISH|FISHING_MATERIAL
tropical_scale|TROPICAL_FISH|FISHING_MATERIAL
nautilus_core|NAUTILUS_SHELL|FISHING_MATERIAL
blaze_heat_core|BLAZE_POWDER|MONSTER_MATERIAL
nether_wart_extract|NETHER_WART|MAGIC_MATERIAL
soul_sand_core|SOUL_SAND|MAGIC_MATERIAL
glowstone_focus|GLOWSTONE_DUST|MAGIC_MATERIAL
blackstone_guard_plate|POLISHED_BLACKSTONE|MINING_MATERIAL
end_pearl_core|ENDER_PEARL|MAGIC_MATERIAL
chorus_magic_fiber|CHORUS_FRUIT|MAGIC_MATERIAL
purpur_gem|PURPUR_BLOCK|MAGIC_MATERIAL
dragon_breath_residue|DRAGON_BREATH|MAGIC_MATERIAL
obsidian_handle|OBSIDIAN|MINING_MATERIAL
crying_core|CRYING_OBSIDIAN|MAGIC_MATERIAL
copper_wire_bundle|COPPER_INGOT|MINING_MATERIAL
coal_forged_plate|COAL_BLOCK|MINING_MATERIAL
flint_edge|FLINT|MINING_MATERIAL
clay_mold|CLAY_BALL|MINING_MATERIAL
sand_glass_core|GLASS|MINING_MATERIAL
terracotta_plate|TERRACOTTA|MINING_MATERIAL
dye_essence|MAGENTA_DYE|FARMING_MATERIAL
flower_pollen|YELLOW_DYE|FARMING_MATERIAL
""";

    private static final String RECIPES="""
coal_forged_helmet|Coal Forged Helmet|IRON_HELMET|ARMOR|coal|3|c:coal_forged_plate*16,c:reinforced_hide*12
copper_wire_wand|Copper Wire Wand|BLAZE_ROD|WAND|copper|3|c:copper_wire_bundle*24,c:redstone_circuit_core*4,m:STICK*1
gleaming_iron_sword|Gleaming Iron Sword|IRON_SWORD|SWORD|iron|5|c:gleaming_iron_core*16,c:enchanted_iron_ingot*48,m:STICK*1
golden_thread_necklace|Golden Thread Necklace|GOLD_INGOT|NECKLACE|gold|4|c:golden_thread*24,c:royal_gold_plate*8
diamond_cluster_katana|Diamond Cluster Katana|DIAMOND_SWORD|KATANA|diamond|5|c:diamond_shard_cluster*24,c:amethyst_blade_core*8,m:STICK*1
emerald_inlay_charm|Emerald Inlay Charm|EMERALD|CHARM|emerald|4|c:emerald_inlay*16,c:ancient_guard_plate*4
lapis_spell_staff|Lapis Spell Staff|BLAZE_ROD|STAFF|lapis|5|c:lapis_spell_thread*32,c:quartz_prism*8,m:BLAZE_ROD*1
redstone_circuit_bow|Redstone Circuit Bow|BOW|BOW|redstone|5|c:redstone_circuit_core*16,c:hardened_string*32,m:STICK*3
quartz_prism_ring|Quartz Prism Ring|QUARTZ|RING|quartz|4|c:quartz_prism*16,m:GOLD_NUGGET*32
amethyst_blade_dagger|Amethyst Blade Dagger|IRON_SWORD|DAGGER|amethyst|5|c:amethyst_blade_core*16,c:flint_edge*12,m:STICK*1
compressed_stone_chestplate|Compressed Stone Chestplate|IRON_CHESTPLATE|ARMOR|stone|4|c:compressed_stone_plate*32,c:reinforced_hide*16
cobble_guard_totem|Cobble Guard Totem|TOTEM_OF_UNDYING|TOTEM|cobblestone|4|c:compressed_cobblestone*48,c:compressed_cobblestone*48,m:TOTEM_OF_UNDYING*1
flint_edge_dagger|Flint Edge Dagger|IRON_SWORD|DAGGER|gravel|3|c:flint_edge*24,m:STICK*1
sand_glass_wand|Sand Glass Wand|BLAZE_ROD|WAND|sand|3|c:sand_glass_core*24,c:glowstone_focus*4,m:STICK*1
clay_mold_ring|Clay Mold Ring|CLAY_BALL|RING|clay|3|c:clay_mold*16,c:terracotta_plate*8
terracotta_guard_leggings|Terracotta Guard Leggings|LEATHER_LEGGINGS|ARMOR|terracotta|4|c:terracotta_plate*32,c:reinforced_hide*16
mossy_fiber_boots|Mossy Fiber Boots|LEATHER_BOOTS|ARMOR|moss_block|3|c:mossy_fiber*24,c:reinforced_leather*12
icebound_charm|Icebound Charm|PACKED_ICE|CHARM|ice|3|c:frosted_scale*12,c:frosted_leaf*16
obsidian_handle_broadsword|Obsidian Handle Broadsword|DIAMOND_SWORD|SWORD|obsidian|4|c:obsidian_handle*1,c:obsidian_plate*24,c:diamond_edge_fragment*8
crying_core_totem|Crying Core Totem|TOTEM_OF_UNDYING|TOTEM|crying_obsidian|4|c:crying_core*16,c:void_resistance_shard*8,m:TOTEM_OF_UNDYING*1
ancient_log_staff|Ancient Log Staff|BLAZE_ROD|STAFF|oak_log|5|c:ancient_log_handle*1,c:ancient_oak_core*32,c:mana_thread*12
spruce_guard_bow|Spruce Guard Bow|BOW|BOW|spruce_log|4|c:ancient_log_handle*1,c:hardened_string*32,c:enchanted_feather*8
birch_scout_dagger|Birch Scout Dagger|IRON_SWORD|DAGGER|birch_log|4|c:ancient_log_handle*1,c:refined_bamboo*16,c:flint_edge*8
jungle_vine_whip|Jungle Vine Whip|IRON_SWORD|SWORD|jungle_log|4|c:jungle_vine_rope*32,c:ancient_oak_core*12,m:STICK*1
mangrove_resin_charm|Mangrove Resin Charm|MANGROVE_ROOTS|CHARM|mangrove_log|3|c:mangrove_resin*24,c:mossy_fiber*12
cherry_petals_ring|Cherry Petals Ring|PINK_PETALS|RING|cherry_log|3|c:cherry_petals_bundle*24,m:GOLD_NUGGET*24
frosted_leaf_necklace|Frosted Leaf Necklace|AZALEA_LEAVES|NECKLACE|oak_leaves|4|c:frosted_leaf*32,c:moonlit_silk*8
bamboo_guard_katana|Bamboo Guard Katana|IRON_SWORD|KATANA|bamboo|4|c:refined_bamboo*48,c:bamboo_fiber*32,m:STICK*1
vinebound_boots|Vinebound Boots|LEATHER_BOOTS|ARMOR|vine|3|c:jungle_vine_rope*24,c:reinforced_hide*16
woodland_totem|Woodland Totem|TOTEM_OF_UNDYING|TOTEM|oak_log|5|c:ancient_oak_core*24,c:ancient_log_handle*1,m:TOTEM_OF_UNDYING*1
wheat_charm|Wheat Charm|WHEAT|CHARM|wheat|3|c:wheat_charm_core*16,c:enchanted_wheat*16
sugar_crystal_wand|Sugar Crystal Wand|BLAZE_ROD|WAND|sugar_cane|4|c:sugar_crystal*24,c:crystalized_sugar*16,m:STICK*1
melon_juice_food|Melon Juice Food|MELON_SLICE|FOOD|melon|4|c:melon_juice_core*16,c:enchanted_melon_chunk*8
pumpkin_shell_helmet|Pumpkin Shell Helmet|LEATHER_HELMET|ARMOR|pumpkin|4|c:pumpkin_shell_plate*24,c:reinforced_hide*12
beetroot_life_potion|Beetroot Life Potion|POTION|POTION|beetroot|4|c:beetroot_life_extract*24,m:GLASS_BOTTLE*1
carrot_spirit_ring|Carrot Spirit Ring|GOLDEN_CARROT|RING|carrot|4|c:carrot_spirit*24,c:carrot_gold_mix*8
potato_guard_charm|Potato Guard Charm|POTATO|CHARM|potato|3|c:potato_guard_starch*24,c:potato_starch*16
honeyed_silk_necklace|Honeyed Silk Necklace|HONEYCOMB|NECKLACE|honey_bottle|4|c:honeyed_cloth*24,c:moonlit_silk*8
cactus_spine_bow|Cactus Spine Bow|BOW|BOW|cactus|4|c:cactus_spine_bundle*32,c:hardened_string*24,m:STICK*3
glow_berry_staff|Glow Berry Staff|BLAZE_ROD|STAFF|glow_berries|4|c:glow_berry_extract*24,c:lapis_spell_thread*8,m:STICK*1
bone_guard_chestplate|Bone Guard Chestplate|IRON_CHESTPLATE|ARMOR|bone|5|c:bone_guard_plate*32,c:enchanted_monster_bone*24
string_tension_bow|String Tension Bow|BOW|BOW|string|5|c:string_tension_core*24,c:spider_silk_bundle*16,m:STICK*3
spider_venom_dagger|Spider Venom Dagger|IRON_SWORD|DAGGER|spider_eye|4|c:spider_venom_glass*24,c:toxic_vial*12,m:STICK*1
ender_eye_wand|Ender Eye Wand|ENDER_EYE|WAND|ender_pearl|5|c:ender_eye_fragment*16,c:void_pearl_fragment*12,m:STICK*1
slime_binding_totem|Slime Binding Totem|TOTEM_OF_UNDYING|TOTEM|slime_ball|4|c:slime_binding_gel*32,c:slime_core*16,m:TOTEM_OF_UNDYING*1
magma_fang_sword|Magma Fang Sword|NETHERITE_SWORD|SWORD|magma_cream|5|c:magma_fang*24,c:magma_core*24,m:STICK*1
ghast_soul_staff|Ghast Soul Staff|BLAZE_ROD|STAFF|ghast_tear|4|c:ghast_soul_thread*16,c:mana_thread*8,m:BLAZE_ROD*1
phantom_cloth_boots|Phantom Cloth Boots|LEATHER_BOOTS|ARMOR|phantom_membrane|4|c:phantom_cloth*24,c:phantom_wing*16
rabbit_luck_charm|Rabbit Luck Charm|RABBIT_FOOT|CHARM|rabbit_foot|4|c:rabbit_luck_foot*16,c:luck_fragment*12
guardian_scale_shield_charm|Guardian Scale Shield Charm|PRISMARINE|CHARM|prismarine_shard|5|c:guardian_scale_plate*24,c:prismarine_guard_scale*24
prismarine_spell_staff|Prismarine Spell Staff|BLAZE_ROD|STAFF|prismarine_crystals|4|c:prismarine_spell_core*24,c:sea_lantern_core*12,m:STICK*1
kelp_rope_fishing_rod|Kelp Rope Fishing Rod|FISHING_ROD|FISHING_ROD|kelp|4|c:kelp_rope*32,m:STRING*32,m:STICK*3
cod_oil_food|Cod Oil Food|COOKED_COD|FOOD|cod|3|c:cod_oil*16,m:COD*32
salmon_scale_leggings|Salmon Scale Leggings|LEATHER_LEGGINGS|ARMOR|salmon|4|c:salmon_scale*32,c:reinforced_hide*16
puffer_spike_dagger|Puffer Spike Dagger|IRON_SWORD|DAGGER|pufferfish|4|c:puffer_spike*24,c:toxic_dust*12,m:STICK*1
tropical_scale_ring|Tropical Scale Ring|TROPICAL_FISH|RING|tropical_fish|4|c:tropical_scale*24,c:pearl_scale*8
nautilus_core_necklace|Nautilus Core Necklace|NAUTILUS_SHELL|NECKLACE|nautilus_shell|5|c:nautilus_core*12,c:storm_pearl*4
sea_lantern_totem|Sea Lantern Totem|TOTEM_OF_UNDYING|TOTEM|sea_lantern|4|c:sea_lantern_core*24,m:TOTEM_OF_UNDYING*1
guardian_eye_bow|Guardian Eye Bow|BOW|BOW|prismarine_shard|4|c:guardian_eye*12,c:hardened_string*32,m:STICK*3
storm_fisher_charm|Storm Fisher Charm|HEART_OF_THE_SEA|CHARM|cod|5|c:storm_pearl*8,c:pearl_scale*24
""";

    static void apply(ConfigurationSection root){RequestedCollectionRecipeDefaults.applyDataset(root,MATERIALS,RECIPES);}
    static Set<String> recipeIds(){return RequestedCollectionRecipeDefaults.ids(RECIPES);}
}
