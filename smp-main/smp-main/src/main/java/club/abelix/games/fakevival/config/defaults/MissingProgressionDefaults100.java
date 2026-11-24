package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

/** Idempotent Java registry batch for the requested one hundred vanilla-path recipes. */
public final class MissingProgressionDefaults100 {
    private MissingProgressionDefaults100() {}

    // id|Bukkit base material|subcategory
    private static final String MATERIALS = """
cobble_reinforced_core|COBBLESTONE|BUILDING
deepslate_pressure_plate|COBBLED_DEEPSLATE|BUILDING
smooth_stone_core|SMOOTH_STONE|BUILDING
stone_brick_guard_plate|STONE_BRICKS|BUILDING
sandstone_sun_core|SANDSTONE|BUILDING
red_sand_heat_core|RED_SAND|BUILDING
red_sandstone_plate|RED_SANDSTONE|BUILDING
clear_glass_lens|GLASS|BUILDING
glass_pane_shard|GLASS_PANE|BUILDING
brick_binding_core|BRICK|BUILDING
nether_brick_plate|NETHER_BRICK|NETHER
netherrack_flame_chunk|NETHERRACK|NETHER
crimson_stem_fiber|CRIMSON_STEM|NETHER
warped_stem_fiber|WARPED_STEM|NETHER
crimson_fungus_spore|CRIMSON_FUNGUS|NETHER
warped_fungus_spore|WARPED_FUNGUS|NETHER
shroomlight_glow_core|SHROOMLIGHT|NETHER
glowstone_spark_core|GLOWSTONE_DUST|NETHER
bone_block_fragment|BONE_BLOCK|MOB_DROPS
white_wool_padding|WHITE_WOOL|NATURAL
colored_wool_thread|RED_WOOL|NATURAL
leather_guard_strip|LEATHER|MOB_DROPS
feather_light_core|FEATHER|MOB_DROPS
ink_shadow_drop|INK_SAC|MOB_DROPS
glow_ink_light_drop|GLOW_INK_SAC|MOB_DROPS
turtle_shell_chip|TURTLE_SCUTE|MOB_DROPS
armadillo_shell_chip|ARMADILLO_SCUTE|MOB_DROPS
ancient_sniffer_scale|SNIFFER_EGG|NATURAL
torchflower_pollen|TORCHFLOWER|DYES_FLOWERS
pitcher_plant_fiber|PITCHER_PLANT|DYES_FLOWERS
bamboo_joint|BAMBOO|WOOD
sugar_cane_paper_core|SUGAR_CANE|FARMING
haybound_fiber|HAY_BLOCK|FARMING
apple_sweet_core|APPLE|FARMING
sweet_berry_core|SWEET_BERRIES|FARMING
beetroot_red_core|BEETROOT|FARMING
potato_starch_core|POTATO|FARMING
carrot_gold_core|CARROT|FARMING
wheat_grain_core|WHEAT|FARMING
beef_power_strip|COOKED_BEEF|FARMING
pork_strength_strip|COOKED_PORKCHOP|FARMING
chicken_light_strip|COOKED_CHICKEN|FARMING
mutton_warm_strip|COOKED_MUTTON|FARMING
rabbit_swift_strip|COOKED_RABBIT|FARMING
paper_spell_sheet|PAPER|UTILITY
book_rune_page|BOOK|UTILITY
slime_block_gel|SLIME_BLOCK|MOB_DROPS
honey_block_glaze|HONEY_BLOCK|FARMING
copper_block_wire|COPPER_BLOCK|REDSTONE
iron_block_plate|IRON_BLOCK|BUILDING
gold_block_gear|GOLD_BLOCK|UTILITY
diamond_block_lens|DIAMOND_BLOCK|UTILITY
emerald_block_gem|EMERALD_BLOCK|UTILITY
lapis_block_dust|LAPIS_BLOCK|UTILITY
redstone_block_motor|REDSTONE_BLOCK|REDSTONE
quartz_block_frame|QUARTZ_BLOCK|BUILDING
amethyst_block_pulse|AMETHYST_BLOCK|UTILITY
raw_iron_chunk|RAW_IRON|NATURAL
raw_gold_chunk|RAW_GOLD|NATURAL
raw_copper_chunk|RAW_COPPER|NATURAL
iron_nugget_pin|IRON_NUGGET|UTILITY
gold_nugget_ring|GOLD_NUGGET|UTILITY
chain_link_core|CHAIN|UTILITY
lantern_glow_frame|LANTERN|UTILITY
compass_needle_core|COMPASS|UTILITY
clock_gear_core|CLOCK|UTILITY
rail_steel_strip|RAIL|REDSTONE
powered_rail_motor|POWERED_RAIL|REDSTONE
activator_rail_switch|ACTIVATOR_RAIL|REDSTONE
detector_rail_sensor|DETECTOR_RAIL|REDSTONE
minecart_wheel_core|MINECART|REDSTONE
hopper_funnel_core|HOPPER|REDSTONE
redstone_torch_spark|REDSTONE_TORCH|REDSTONE
repeater_delay_core|REPEATER|REDSTONE
comparator_signal_core|COMPARATOR|REDSTONE
observer_eye_core|OBSERVER|REDSTONE
piston_push_core|PISTON|REDSTONE
sticky_piston_gel|STICKY_PISTON|REDSTONE
dispenser_launch_core|DISPENSER|REDSTONE
dropper_release_core|DROPPER|REDSTONE
target_focus_core|TARGET|REDSTONE
sculk_growth_core|SCULK|NATURAL
sculk_sensor_core|SCULK_SENSOR|REDSTONE
echo_shard_core|ECHO_SHARD|UTILITY
disc_fragment_core|DISC_FRAGMENT_5|UTILITY
nether_star_dust|NETHER_STAR|END
dragon_breath_core|DRAGON_BREATH|END
dragon_head_scale|DRAGON_HEAD|END
elytra_wing_fiber|ELYTRA|END
firework_rocket_core|FIREWORK_ROCKET|UTILITY
firework_star_core|FIREWORK_STAR|UTILITY
experience_bottle_core|EXPERIENCE_BOTTLE|UTILITY
name_tag_thread|NAME_TAG|UTILITY
saddle_leather_core|SADDLE|UTILITY
lead_rope_core|LEAD|UTILITY
totem_spirit_core|TOTEM_OF_UNDYING|MOB_DROPS
trident_prong_core|TRIDENT|FISHING
heart_sea_core|HEART_OF_THE_SEA|FISHING
spore_blossom_pollen|SPORE_BLOSSOM|DYES_FLOWERS
rooted_dirt_core|ROOTED_DIRT|NATURAL
hanging_root_fiber|HANGING_ROOTS|NATURAL
big_dripleaf_fiber|BIG_DRIPLEAF|NATURAL
small_dripleaf_fiber|SMALL_DRIPLEAF|NATURAL
azalea_root_core|AZALEA|NATURAL
flowering_azalea_petal|FLOWERING_AZALEA|DYES_FLOWERS
""";

    private static final String SUPPORT = """
netherite_rivet|NETHERITE_SCRAP|NETHER
bone_armor_link|BONE|MOB_DROPS
lapis_spell_dust|LAPIS_LAZULI|UTILITY
molten_gold_core|GOLD_INGOT|UTILITY
diamond_lens|DIAMOND|UTILITY
emerald_locket|EMERALD|UTILITY
redstone_motor|REDSTONE_BLOCK|REDSTONE
quartz_casing|QUARTZ|UTILITY
string_woven_core|STRING|MOB_DROPS
""";

    // id|category|icon|tracked Bukkit materials|rare
    private static final String COLLECTIONS = """
cobblestone|mining|COBBLESTONE|COBBLESTONE|false
cobbled_deepslate|mining|COBBLED_DEEPSLATE|COBBLED_DEEPSLATE|false
smooth_stone|mining|SMOOTH_STONE|SMOOTH_STONE|false
stone_bricks|mining|STONE_BRICKS|STONE_BRICKS|false
sandstone|mining|SANDSTONE|SANDSTONE|false
red_sand|mining|RED_SAND|RED_SAND|false
red_sandstone|mining|RED_SANDSTONE|RED_SANDSTONE|false
glass|mining|GLASS|GLASS|false
glass_pane|mining|GLASS_PANE|GLASS_PANE|false
brick|mining|BRICK|BRICK,BRICKS|false
nether_brick|mining|NETHER_BRICK|NETHER_BRICK,NETHER_BRICKS|false
netherrack|mining|NETHERRACK|NETHERRACK|false
crimson_stem|foraging|CRIMSON_STEM|CRIMSON_STEM|false
warped_stem|foraging|WARPED_STEM|WARPED_STEM|false
crimson_fungus|foraging|CRIMSON_FUNGUS|CRIMSON_FUNGUS|false
warped_fungus|foraging|WARPED_FUNGUS|WARPED_FUNGUS|false
shroomlight|foraging|SHROOMLIGHT|SHROOMLIGHT|false
glowstone|mining|GLOWSTONE|GLOWSTONE,GLOWSTONE_DUST|false
bone_block|combat|BONE_BLOCK|BONE_BLOCK|false
white_wool|farming|WHITE_WOOL|WHITE_WOOL|false
colored_wool|farming|RED_WOOL|ORANGE_WOOL,MAGENTA_WOOL,LIGHT_BLUE_WOOL,YELLOW_WOOL,LIME_WOOL,PINK_WOOL,GRAY_WOOL,LIGHT_GRAY_WOOL,CYAN_WOOL,PURPLE_WOOL,BLUE_WOOL,BROWN_WOOL,GREEN_WOOL,RED_WOOL,BLACK_WOOL|false
leather|combat|LEATHER|LEATHER|false
feather|combat|FEATHER|FEATHER|false
ink_sac|fishing|INK_SAC|INK_SAC|false
glow_ink_sac|fishing|GLOW_INK_SAC|GLOW_INK_SAC|false
turtle_scute|fishing|TURTLE_SCUTE|TURTLE_SCUTE|true
armadillo_scute|combat|ARMADILLO_SCUTE|ARMADILLO_SCUTE|true
sniffer_egg|farming|SNIFFER_EGG|SNIFFER_EGG|true
torchflower|farming|TORCHFLOWER|TORCHFLOWER|false
pitcher_plant|farming|PITCHER_PLANT|PITCHER_PLANT|false
bamboo|foraging|BAMBOO|BAMBOO|false
sugar_cane|farming|SUGAR_CANE|SUGAR_CANE|false
hay_block|farming|HAY_BLOCK|HAY_BLOCK|false
apple|farming|APPLE|APPLE|false
sweet_berries|farming|SWEET_BERRIES|SWEET_BERRIES|false
beetroot|farming|BEETROOT|BEETROOT|false
potato|farming|POTATO|POTATO,BAKED_POTATO|false
carrot|farming|CARROT|CARROT,GOLDEN_CARROT|false
wheat|farming|WHEAT|WHEAT|false
cooked_beef|farming|COOKED_BEEF|COOKED_BEEF|false
cooked_porkchop|farming|COOKED_PORKCHOP|COOKED_PORKCHOP|false
cooked_chicken|farming|COOKED_CHICKEN|COOKED_CHICKEN|false
mutton|farming|COOKED_MUTTON|COOKED_MUTTON|false
rabbit|farming|COOKED_RABBIT|COOKED_RABBIT|false
paper|farming|PAPER|PAPER|false
book|magic|BOOK|BOOK|false
slime_block|combat|SLIME_BLOCK|SLIME_BLOCK|false
honey_block|farming|HONEY_BLOCK|HONEY_BLOCK|false
copper_block|mining|COPPER_BLOCK|COPPER_BLOCK|false
iron_block|mining|IRON_BLOCK|IRON_BLOCK|false
gold_block|mining|GOLD_BLOCK|GOLD_BLOCK|false
diamond_block|mining|DIAMOND_BLOCK|DIAMOND_BLOCK|false
emerald_block|mining|EMERALD_BLOCK|EMERALD_BLOCK|false
lapis_block|mining|LAPIS_BLOCK|LAPIS_BLOCK|false
redstone_block|mining|REDSTONE_BLOCK|REDSTONE_BLOCK|false
quartz_block|mining|QUARTZ_BLOCK|QUARTZ_BLOCK|false
amethyst_block|mining|AMETHYST_BLOCK|AMETHYST_BLOCK|false
raw_iron|mining|RAW_IRON|RAW_IRON|false
raw_gold|mining|RAW_GOLD|RAW_GOLD|false
raw_copper|mining|RAW_COPPER|RAW_COPPER|false
iron_nugget|mining|IRON_NUGGET|IRON_NUGGET|false
gold_nugget|mining|GOLD_NUGGET|GOLD_NUGGET|false
chain|tools|CHAIN|CHAIN|false
lantern|tools|LANTERN|LANTERN,SOUL_LANTERN|false
compass|tools|COMPASS|COMPASS|false
clock|tools|CLOCK|CLOCK|false
rail|tools|RAIL|RAIL|false
powered_rail|tools|POWERED_RAIL|POWERED_RAIL|false
activator_rail|tools|ACTIVATOR_RAIL|ACTIVATOR_RAIL|false
detector_rail|tools|DETECTOR_RAIL|DETECTOR_RAIL|false
minecart|tools|MINECART|MINECART|false
hopper|tools|HOPPER|HOPPER|false
redstone_torch|tools|REDSTONE_TORCH|REDSTONE_TORCH|false
repeater|tools|REPEATER|REPEATER|false
comparator|tools|COMPARATOR|COMPARATOR|false
observer|tools|OBSERVER|OBSERVER|false
piston|tools|PISTON|PISTON|false
sticky_piston|tools|STICKY_PISTON|STICKY_PISTON|false
dispenser|tools|DISPENSER|DISPENSER|false
dropper|tools|DROPPER|DROPPER|false
target_block|tools|TARGET|TARGET|false
sculk|mining|SCULK|SCULK|false
sculk_sensor|mining|SCULK_SENSOR|SCULK_SENSOR|true
echo_shard|mining|ECHO_SHARD|ECHO_SHARD|true
disc_fragment|tools|DISC_FRAGMENT_5|DISC_FRAGMENT_5|true
nether_star|combat|NETHER_STAR|NETHER_STAR|true
dragon_breath|magic|DRAGON_BREATH|DRAGON_BREATH|true
dragon_head|combat|DRAGON_HEAD|DRAGON_HEAD|true
elytra|tools|ELYTRA|ELYTRA|true
firework_rocket|tools|FIREWORK_ROCKET|FIREWORK_ROCKET|false
firework_star|tools|FIREWORK_STAR|FIREWORK_STAR|false
experience_bottle|magic|EXPERIENCE_BOTTLE|EXPERIENCE_BOTTLE|false
name_tag|tools|NAME_TAG|NAME_TAG|true
saddle|tools|SADDLE|SADDLE|true
lead|tools|LEAD|LEAD|false
totem|combat|TOTEM_OF_UNDYING|TOTEM_OF_UNDYING|true
trident|fishing|TRIDENT|TRIDENT|true
shell|fishing|NAUTILUS_SHELL|NAUTILUS_SHELL|true
heart_of_the_sea|fishing|HEART_OF_THE_SEA|HEART_OF_THE_SEA|true
spore_blossom|foraging|SPORE_BLOSSOM|SPORE_BLOSSOM|false
rooted_dirt|foraging|ROOTED_DIRT|ROOTED_DIRT|false
hanging_roots|foraging|HANGING_ROOTS|HANGING_ROOTS|false
big_dripleaf|foraging|BIG_DRIPLEAF|BIG_DRIPLEAF|false
small_dripleaf|foraging|SMALL_DRIPLEAF|SMALL_DRIPLEAF|false
azalea|foraging|AZALEA|AZALEA|false
flowering_azalea|foraging|FLOWERING_AZALEA|FLOWERING_AZALEA|false
""";

    // id|base|type|collection|tier|ingredient identities
    private static final String RECIPES = """
cobble_reinforced_pickaxe|DIAMOND_PICKAXE|PICKAXE|cobblestone|4|c:cobble_reinforced_core*24,m:STICK*2
deepslate_pressure_chestplate|IRON_CHESTPLATE|ARMOR|cobbled_deepslate|5|c:deepslate_pressure_plate*32,c:reinforced_hide*16
smooth_stone_guard_charm|SMOOTH_STONE|CHARM|smooth_stone|4|c:smooth_stone_core*24,c:gold_nugget_ring*8
stone_brick_guard_leggings|IRON_LEGGINGS|ARMOR|stone_bricks|4|c:stone_brick_guard_plate*32,c:reinforced_hide*16
sandstone_sun_wand|BLAZE_ROD|WAND|sandstone|4|c:sandstone_sun_core*24,c:glowstone_spark_core*8,m:STICK*1
red_sand_heat_dagger|IRON_SWORD|DAGGER|red_sand|4|c:red_sand_heat_core*24,c:flint_edge*8,m:STICK*1
red_sandstone_boots|IRON_BOOTS|ARMOR|red_sandstone|4|c:red_sandstone_plate*32,c:reinforced_leather*12
clear_glass_lens_ring|GLASS|RING|glass|4|c:clear_glass_lens*24,c:quartz_mirror*4
glass_pane_shard_dagger|IRON_SWORD|DAGGER|glass_pane|3|c:glass_pane_shard*32,m:STICK*1
brick_binding_totem|TOTEM_OF_UNDYING|TOTEM|brick|4|c:brick_binding_core*24,m:TOTEM_OF_UNDYING*1
nether_brick_plate_helmet|IRON_HELMET|ARMOR|nether_brick|5|c:nether_brick_plate*32,c:blaze_rod_shard*12
netherrack_flame_sword|IRON_SWORD|SWORD|netherrack|4|c:netherrack_flame_chunk*32,c:magma_heat_scale*8,m:STICK*1
crimson_stem_axe|DIAMOND_AXE|AXE|crimson_stem|4|c:crimson_stem_fiber*32,c:netherite_rivet*4,m:STICK*2
warped_stem_staff|BLAZE_ROD|STAFF|warped_stem|4|c:warped_stem_fiber*32,c:ender_thread*8,m:BLAZE_ROD*1
crimson_fungus_charm|CRIMSON_FUNGUS|CHARM|crimson_fungus|3|c:crimson_fungus_spore*24,c:soul_sand_essence*8
warped_fungus_ring|WARPED_FUNGUS|RING|warped_fungus|3|c:warped_fungus_spore*24,c:ender_thread*8
shroomlight_glow_wand|BLAZE_ROD|WAND|shroomlight|4|c:shroomlight_glow_core*24,c:glowstone_spark_core*16,m:STICK*1
glowstone_spark_staff|BLAZE_ROD|STAFF|glowstone|5|c:glowstone_spark_core*32,c:quartz_focus_lens*8,m:BLAZE_ROD*1
bone_block_helmet|IRON_HELMET|ARMOR|bone_block|4|c:bone_block_fragment*32,c:bone_armor_link*12
white_wool_boots|LEATHER_BOOTS|ARMOR|white_wool|3|c:white_wool_padding*32,c:reinforced_leather*8
colored_wool_charm|RED_WOOL|CHARM|colored_wool|3|c:colored_wool_thread*32,c:dye_magic_dust*8
leather_guard_chestplate|LEATHER_CHESTPLATE|ARMOR|leather|4|c:leather_guard_strip*32,c:reinforced_leather*16
feather_light_boots|LEATHER_BOOTS|ARMOR|feather|4|c:feather_light_core*32,c:rabbit_swiftness_foot*8
ink_shadow_dagger|IRON_SWORD|DAGGER|ink_sac|4|c:ink_shadow_drop*24,c:shadow_dust*8,m:STICK*1
glow_ink_light_wand|BLAZE_ROD|WAND|glow_ink_sac|4|c:glow_ink_light_drop*24,c:glowstone_spark_core*8,m:STICK*1
turtle_shell_charm|TURTLE_HELMET|CHARM|turtle_scute|5|c:turtle_shell_chip*24,c:nautilus_shell_shard*8
armadillo_shell_leggings|IRON_LEGGINGS|ARMOR|armadillo_scute|4|c:armadillo_shell_chip*32,c:reinforced_hide*12
sniffer_scale_necklace|SNIFFER_EGG|NECKLACE|sniffer_egg|3|c:ancient_sniffer_scale*16,c:gold_nugget_ring*8
torchflower_pollen_staff|BLAZE_ROD|STAFF|torchflower|4|c:torchflower_pollen*24,c:flower_essence*8,m:STICK*1
pitcher_plant_fiber_leggings|LEATHER_LEGGINGS|ARMOR|pitcher_plant|4|c:pitcher_plant_fiber*32,c:leafy_fiber*12
bamboo_joint_spear|IRON_SWORD|SWORD|bamboo|4|c:bamboo_joint*32,c:flint_edge*8,m:STICK*1
sugar_cane_paper_wand|BLAZE_ROD|WAND|sugar_cane|5|c:sugar_cane_paper_core*24,c:paper_spell_sheet*8,m:STICK*1
haybound_chestplate|LEATHER_CHESTPLATE|ARMOR|hay_block|4|c:haybound_fiber*32,c:leather_guard_strip*8
apple_sweet_charm|APPLE|CHARM|apple|4|c:apple_sweet_core*24,c:honey_sticky_core*8
sweet_berry_ring|SWEET_BERRIES|RING|sweet_berries|3|c:sweet_berry_core*24,c:gold_nugget_ring*8
beetroot_red_potion|POTION|POTION|beetroot|5|c:beetroot_red_core*24,m:GLASS_BOTTLE*1
potato_starch_food|BAKED_POTATO|FOOD|potato|4|c:potato_starch_core*16,m:BAKED_POTATO*1
wheat_grain_bread_plus|BREAD|FOOD|wheat|5|c:wheat_grain_core*16,c:enchanted_bread*1
beef_power_food|COOKED_BEEF|FOOD|cooked_beef|4|c:beef_power_strip*16,c:cooked_meat_slice*4
pork_strength_food|COOKED_PORKCHOP|FOOD|cooked_porkchop|4|c:pork_strength_strip*16,c:cooked_meat_slice*4
chicken_light_food|COOKED_CHICKEN|FOOD|cooked_chicken|4|c:chicken_light_strip*16,c:cooked_meat_slice*4
mutton_warm_food|COOKED_MUTTON|FOOD|mutton|4|c:mutton_warm_strip*16,c:cooked_meat_slice*4
rabbit_swift_food|COOKED_RABBIT|FOOD|rabbit|4|c:rabbit_swift_strip*16,c:cooked_meat_slice*4
paper_spell_sheet_wand|BLAZE_ROD|WAND|paper|4|c:paper_spell_sheet*24,c:lapis_spell_dust*8,m:STICK*1
book_rune_staff|BLAZE_ROD|STAFF|book|5|c:book_rune_page*24,c:amethyst_pulse_core*8,m:BLAZE_ROD*1
slime_block_gel_boots|LEATHER_BOOTS|ARMOR|slime_block|4|c:slime_block_gel*32,c:slime_stretch_gel*8
honey_block_glaze_charm|HONEY_BLOCK|CHARM|honey_block|4|c:honey_block_glaze*24,c:honey_sticky_core*8
copper_block_wire_crossbow|CROSSBOW|CROSSBOW|copper_block|5|c:copper_block_wire*32,c:redstone_block_motor*8,m:STICK*3
iron_block_plate_helmet|IRON_HELMET|ARMOR|iron_block|5|c:iron_block_plate*32,c:heavy_iron_plate*16
gold_block_gear_ring|GOLD_BLOCK|RING|gold_block|5|c:gold_block_gear*24,c:molten_gold_core*8
diamond_block_lens_wand|BLAZE_ROD|WAND|diamond_block|6|c:diamond_block_lens*24,c:diamond_lens*8,m:STICK*1
emerald_block_gem_necklace|EMERALD|NECKLACE|emerald_block|6|c:emerald_block_gem*24,c:emerald_locket*8
lapis_block_dust_staff|BLAZE_ROD|STAFF|lapis_block|5|c:lapis_block_dust*32,c:lapis_spell_dust*16,m:BLAZE_ROD*1
redstone_block_motor_drill|NETHERITE_PICKAXE|DRILL|redstone_block|6|c:redstone_block_motor*24,c:redstone_motor*12,c:copper_tech_wire*16
quartz_block_frame_staff|BLAZE_ROD|STAFF|quartz_block|5|c:quartz_block_frame*24,c:quartz_casing*8,m:BLAZE_ROD*1
amethyst_block_pulse_charm|AMETHYST_BLOCK|CHARM|amethyst_block|5|c:amethyst_block_pulse*24,c:amethyst_pulse_core*8
raw_iron_chunk_sword|IRON_SWORD|SWORD|raw_iron|4|c:raw_iron_chunk*32,m:STICK*1
raw_gold_chunk_charm|RAW_GOLD|CHARM|raw_gold|4|c:raw_gold_chunk*24,c:gold_nugget_ring*8
raw_copper_chunk_pickaxe|IRON_PICKAXE|PICKAXE|raw_copper|4|c:raw_copper_chunk*32,m:STICK*2
iron_nugget_pin_dagger|IRON_SWORD|DAGGER|iron_nugget|3|c:iron_nugget_pin*32,m:STICK*1
gold_nugget_ring_plus|GOLD_NUGGET|RING|gold_nugget|3|c:gold_nugget_ring*24,c:molten_gold_core*4
chain_link_leggings|CHAINMAIL_LEGGINGS|ARMOR|chain|4|c:chain_link_core*32,c:iron_nugget_pin*16
lantern_glow_charm|LANTERN|CHARM|lantern|3|c:lantern_glow_frame*24,c:glowstone_spark_core*4
compass_needle_necklace|COMPASS|NECKLACE|compass|4|c:compass_needle_core*24,c:redstone_torch_spark*8
clock_gear_ring|CLOCK|RING|clock|4|c:clock_gear_core*24,c:gold_block_gear*8
rail_steel_boots|IRON_BOOTS|ARMOR|rail|3|c:rail_steel_strip*32,c:iron_nugget_pin*12
powered_rail_motor_boots|GOLDEN_BOOTS|ARMOR|powered_rail|4|c:powered_rail_motor*24,c:redstone_block_motor*8
activator_rail_switch_wand|BLAZE_ROD|WAND|activator_rail|3|c:activator_rail_switch*24,c:redstone_torch_spark*8,m:STICK*1
detector_rail_sensor_charm|DETECTOR_RAIL|CHARM|detector_rail|3|c:detector_rail_sensor*24,c:comparator_signal_core*8
minecart_wheel_charm|MINECART|CHARM|minecart|3|c:minecart_wheel_core*24,c:rail_steel_strip*8
hopper_funnel_totem|TOTEM_OF_UNDYING|TOTEM|hopper|4|c:hopper_funnel_core*24,m:TOTEM_OF_UNDYING*1
redstone_torch_spark_wand|BLAZE_ROD|WAND|redstone_torch|3|c:redstone_torch_spark*24,m:STICK*1
repeater_delay_staff|BLAZE_ROD|STAFF|repeater|4|c:repeater_delay_core*24,c:redstone_torch_spark*8,m:BLAZE_ROD*1
comparator_signal_ring|COMPARATOR|RING|comparator|4|c:comparator_signal_core*24,c:quartz_mirror*4
observer_eye_helmet|IRON_HELMET|ARMOR|observer|4|c:observer_eye_core*24,c:clear_glass_lens*8
piston_push_gauntlet|IRON_SWORD|SWORD|piston|4|c:piston_push_core*32,c:iron_block_plate*8,m:STICK*1
sticky_piston_gel_charm|STICKY_PISTON|CHARM|sticky_piston|4|c:sticky_piston_gel*24,c:slime_block_gel*8
dispenser_launch_bow|BOW|BOW|dispenser|4|c:dispenser_launch_core*24,c:string_woven_core*8,m:STICK*3
dropper_release_dagger|IRON_SWORD|DAGGER|dropper|3|c:dropper_release_core*24,m:STICK*1
target_focus_crossbow|CROSSBOW|CROSSBOW|target_block|4|c:target_focus_core*24,c:redstone_torch_spark*8,m:STICK*3
sculk_growth_boots|LEATHER_BOOTS|ARMOR|sculk|4|c:sculk_growth_core*32,c:echo_shard_core*2
sculk_sensor_wand|BLAZE_ROD|WAND|sculk_sensor|4|c:sculk_sensor_core*24,c:echo_shard_core*2,m:STICK*1
echo_shard_ring|ECHO_SHARD|RING|echo_shard|5|c:echo_shard_core*16,c:sculk_sensor_core*8
disc_fragment_charm|DISC_FRAGMENT_5|CHARM|disc_fragment|3|c:disc_fragment_core*16,c:echo_shard_core*1
nether_star_totem|TOTEM_OF_UNDYING|TOTEM|nether_star|6|c:nether_star_dust*8,c:totem_spirit_core*8,m:TOTEM_OF_UNDYING*1
dragon_breath_staff|END_ROD|STAFF|dragon_breath|6|c:dragon_breath_core*16,c:purpur_rune_plate*8,m:END_ROD*1
dragon_head_helmet|NETHERITE_HELMET|ARMOR|dragon_head|6|c:dragon_head_scale*16,c:end_stone_fragment*16
elytra_wing_charm|ELYTRA|CHARM|elytra|6|c:elytra_wing_fiber*16,c:phantom_shadow_cloth*16
firework_rocket_bow|BOW|BOW|firework_rocket|4|c:firework_rocket_core*24,c:gunpowder_spark_core*8,m:STICK*3
firework_star_ring|FIREWORK_STAR|RING|firework_star|4|c:firework_star_core*24,c:glowstone_spark_core*8
experience_bottle_charm|EXPERIENCE_BOTTLE|CHARM|experience_bottle|4|c:experience_bottle_core*24,c:lapis_block_dust*8
name_tag_necklace|NAME_TAG|NECKLACE|name_tag|4|c:name_tag_thread*24,c:paper_spell_sheet*8
saddle_leather_boots|LEATHER_BOOTS|ARMOR|saddle|4|c:saddle_leather_core*24,c:leather_guard_strip*8
lead_rope_whip|IRON_SWORD|SWORD|lead|4|c:lead_rope_core*24,c:slime_stretch_gel*8,m:STICK*1
totem_spirit_charm|TOTEM_OF_UNDYING|CHARM|totem|5|c:totem_spirit_core*16,c:soul_sand_essence*8
trident_prong_spear|TRIDENT|SWORD|trident|5|c:trident_prong_core*16,c:prismarine_scale_core*16,m:STICK*1
heart_sea_necklace|HEART_OF_THE_SEA|NECKLACE|heart_of_the_sea|5|c:heart_sea_core*16,c:nautilus_shell_shard*8
spore_blossom_charm|SPORE_BLOSSOM|CHARM|spore_blossom|4|c:spore_blossom_pollen*24,c:mossy_thread*8
azalea_growth_staff|BLAZE_ROD|STAFF|azalea|4|c:azalea_root_core*24,c:flowering_azalea_petal*8,m:STICK*1
""";

    private static final int[] COMMON={50,100,250,500,1000,2500,5000,10000,25000,50000};
    private static final int[] RARE={1,5,10,25,50,100,250,500,1000,2500};

    public static void apply(ConfigurationSection root){materials(root,MATERIALS);materials(root,SUPPORT);collections(root);for(String line:RECIPES.strip().split("\\R"))recipe(root,line);}
    public static Set<String> recipeIds(){Set<String>s=new LinkedHashSet<>(ids(RECIPES));s.add("carrot_gold_ring");return Set.copyOf(s);}
    public static Set<String> materialIds(){return ids(MATERIALS);}
    public static Set<String> collectionIds(){return ids(COLLECTIONS);}

    private static void materials(ConfigurationSection root,String data){for(String line:data.strip().split("\\R")){String[]p=line.split("\\|");if(p.length<3)continue;String id=n(p[0]),b="custom-items.items."+id;if(root.isConfigurationSection(b))continue;root.set(b+".name","&f"+pretty(id));root.set(b+".material",p[1]);root.set(b+".type","RECIPE_MATERIAL");root.set(b+".mmo.type","RECIPE_MATERIAL");root.set(b+".source-type",p[2]+"_MATERIAL");root.set(b+".category",browser(p[2]));root.set(b+".collection.category",p[2].toLowerCase(Locale.ROOT));root.set(b+".recipe-material.category",p[2].toLowerCase(Locale.ROOT));root.set(b+".rarity","COMMON");root.set(b+".random-rarity",false);root.set(b+".mmo.random-roll",false);root.set(b+".mmo.stats",null);root.set(b+".lore",List.of("&7A stackable progression material."));root.set(b+".max-stack-size",64);root.set(b+".ability.type","none");root.set(b+".ability.mana-cost",0);root.set(b+".consumable.enabled",false);root.set(b+".unbreakable",false);root.set(b+".tradeable",true);root.set(b+".soulbound",false);root.set(b+".guild-sellable",true);root.set(b+".sellall-allowed",true);}}

    private static void collections(ConfigurationSection root){for(String line:COLLECTIONS.strip().split("\\R")){String[]p=line.split("\\|",-1);String id=n(p[0]),b="collections.items."+id;if(!root.isConfigurationSection(b)){root.set(b+".name","&e"+pretty(id)+" Collection");root.set(b+".category",p[1]);root.set(b+".icon",p[2]);root.set(b+".materials",List.of(p[3].split(",")));}else{if(!root.contains(b+".category"))root.set(b+".category",p[1]);if(!root.contains(b+".icon"))root.set(b+".icon",p[2]);List<String>m=new ArrayList<>(root.getStringList(b+".materials"));for(String v:p[3].split(","))if(m.stream().noneMatch(v::equalsIgnoreCase))m.add(v);root.set(b+".materials",m);}int[]a=Boolean.parseBoolean(p[4])?RARE:COMMON;for(int t=1;t<=10;t++){String q="collections."+p[1]+"."+id+".tiers."+t;if(!root.contains(q+".required"))root.set(q+".required",a[t-1]);if(!root.contains(q+".rewards")||root.getStringList(q+".rewards").isEmpty())root.set(q+".rewards",List.of("perk:tier_"+t));}}}

    private static void recipe(ConfigurationSection root,String line){String[]p=line.split("\\|",6);String id=n(p[0]),item="custom-items.items."+id,r="recipes."+id;if(!root.isConfigurationSection(item)){root.set(item+".name","&a"+pretty(id));root.set(item+".material",p[1]);root.set(item+".type",p[2]);root.set(item+".mmo.type",p[2]);root.set(item+".category",outputCategory(p[2]));root.set(item+".rarity","RARE");root.set(item+".random-rarity",false);root.set(item+".mmo.random-roll",false);root.set(item+".ability.type","none");root.set(item+".ability.mana-cost",0);root.set(item+".unbreakable",!Set.of("FOOD","POTION").contains(p[2]));consumable(root,item,id);}if(!root.isConfigurationSection(r)){root.set(r+".display-name","&a"+pretty(id));root.set(r+".category",recipeCategory(p[2]));root.set(r+".result",id);root.set(r+".unlock.type","COLLECTION");root.set(r+".unlock.collection",n(p[3]));root.set(r+".unlock.tier",Integer.parseInt(p[4]));StringBuilder shape=new StringBuilder();String[]in=p[5].split(",");for(int i=0;i<in.length&&i<3;i++){char c=(char)('A'+i);String[]x=in[i].split("\\*",2);root.set(r+".ingredients."+c,(x[0].startsWith("c:")?"custom:":"material:")+x[0].substring(2));root.set(r+".amounts."+c,Math.clamp(Integer.parseInt(x[1]),1,64));shape.append(c);}while(shape.length()<3)shape.append(' ');root.set(r+".shape",List.of(shape.toString(),"   ","   "));}int tier=Integer.parseInt(p[4]);String category=category(n(p[3])),q="collections."+category+"."+n(p[3])+".tiers."+tier;List<String>rewards=new ArrayList<>(root.getStringList(q+".rewards"));String reward="recipe:"+id;if(!rewards.contains(reward))rewards.add(reward);root.set(q+".rewards",rewards);}

    private static void consumable(ConfigurationSection root,String b,String id){int heal=switch(id){case"potato_starch_food"->110;case"wheat_grain_bread_plus"->150;case"beef_power_food"->160;case"mutton_warm_food"->140;default->0;};boolean enabled=heal>0||Set.of("beetroot_red_potion","pork_strength_food","chicken_light_food","rabbit_swift_food").contains(id);root.set(b+".consumable.enabled",enabled);if(heal>0)root.set(b+".consumable.heal",heal);if(id.equals("beetroot_red_potion")){root.set(b+".consumable.stat-buffs.HEALTH.amount",75);root.set(b+".consumable.stat-buffs.HEALTH.duration-seconds",180);}if(id.equals("pork_strength_food")){root.set(b+".consumable.stat-buffs.STRENGTH.amount",10);root.set(b+".consumable.stat-buffs.STRENGTH.duration-seconds",120);}if(id.equals("chicken_light_food")||id.equals("rabbit_swift_food")){root.set(b+".consumable.stat-buffs.SPEED.amount",id.startsWith("chicken")?5:8);root.set(b+".consumable.stat-buffs.SPEED.duration-seconds",120);}}

    private static String category(String id){for(String line:COLLECTIONS.strip().split("\\R")){String[]p=line.split("\\|",3);if(n(p[0]).equals(id))return p[1];}return"tools";}
    private static String browser(String sub){return switch(sub){case"FARMING"->"farming_materials";case"MOB_DROPS"->"combat_materials";case"FISHING"->"fishing_materials";case"NETHER","END"->"magic_materials";default->"mining_materials";};}
    private static String outputCategory(String type){return switch(type){case"ARMOR"->"armor";case"PICKAXE","AXE","DRILL"->"tools";case"FOOD"->"food";case"POTION"->"potions";case"CHARM","TOTEM","NECKLACE","RING"->"accessories";case"WAND","STAFF"->"magic";default->"weapons";};}
    private static String recipeCategory(String type){return switch(type){case"ARMOR"->"ARMOR";case"PICKAXE","AXE","DRILL"->"TOOLS";case"FOOD"->"FOOD";case"POTION"->"POTIONS";case"CHARM","TOTEM","NECKLACE","RING"->"ACCESSORIES";case"WAND","STAFF"->"MAGIC";default->"WEAPONS";};}
    private static Set<String>ids(String data){Set<String>s=new LinkedHashSet<>();for(String l:data.strip().split("\\R"))s.add(n(l.split("\\|",2)[0]));return Set.copyOf(s);}
    private static String n(String v){return v.toLowerCase(Locale.ROOT).replace('-','_').replace(' ','_');}
    private static String pretty(String v){StringBuilder b=new StringBuilder();for(String p:n(v).split("_")){if(!b.isEmpty())b.append(' ');b.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));}return b.toString();}
}
