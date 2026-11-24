package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;
import java.util.*;

/** Idempotent Java defaults for the requested sixty collection progression recipes. */
public final class RequestedCollectionRecipeDefaults {
    private RequestedCollectionRecipeDefaults() {}

    private static final String MATERIALS="""
reinforced_leather|LEATHER|RECIPE_MATERIAL
hardened_string|STRING|RECIPE_MATERIAL
enchanted_feather|FEATHER|RECIPE_MATERIAL
compressed_cobblestone|COBBLESTONE|MINING_MATERIAL
polished_monster_core|ECHO_SHARD|MONSTER_MATERIAL
glowing_mushroom|BROWN_MUSHROOM|FARMING_MATERIAL
charged_redstone|REDSTONE|MINING_MATERIAL
deep_cave_crystal|AMETHYST_SHARD|MINING_MATERIAL
frosted_scale|PRISMARINE_CRYSTALS|FISHING_MATERIAL
burning_scale|MAGMA_CREAM|MONSTER_MATERIAL
ancient_oak_core|OAK_WOOD|FORAGING_MATERIAL
refined_bamboo|BAMBOO|FORAGING_MATERIAL
silver_fish_scale|PRISMARINE_SHARD|FISHING_MATERIAL
storm_pearl|ENDER_PEARL|MAGIC_MATERIAL
dried_seaweed_bundle|DRIED_KELP|FISHING_MATERIAL
farming_thread|STRING|FARMING_MATERIAL
golden_crop_fiber|YELLOW_WOOL|FARMING_MATERIAL
sharp_bone_shard|BONE|MONSTER_MATERIAL
spider_silk_bundle|WHITE_WOOL|MONSTER_MATERIAL
undead_heart|FERMENTED_SPIDER_EYE|MONSTER_MATERIAL
blazing_powder_core|BLAZE_POWDER|MONSTER_MATERIAL
void_pearl_fragment|ENDER_EYE|MAGIC_MATERIAL
crystalized_sugar|SUGAR|FARMING_MATERIAL
enchanted_pumpkin_chunk|PUMPKIN|FARMING_MATERIAL
enchanted_melon_chunk|MELON_SLICE|FARMING_MATERIAL
heavy_iron_plate|HEAVY_WEIGHTED_PRESSURE_PLATE|MINING_MATERIAL
royal_gold_plate|LIGHT_WEIGHTED_PRESSURE_PLATE|MINING_MATERIAL
diamond_edge_fragment|DIAMOND|MINING_MATERIAL
emerald_guard_gem|EMERALD|MINING_MATERIAL
lapis_mana_core|LAPIS_LAZULI|MAGIC_MATERIAL
redstone_power_cell|REDSTONE_BLOCK|MINING_MATERIAL
quartz_focus_lens|QUARTZ|MAGIC_MATERIAL
amethyst_focus_lens|AMETHYST_SHARD|MAGIC_MATERIAL
nether_brick_core|NETHER_BRICK|MINING_MATERIAL
end_stone_core|END_STONE|MINING_MATERIAL
prismarine_guard_scale|PRISMARINE_SHARD|FISHING_MATERIAL
dark_prismarine_shard|DARK_PRISMARINE|FISHING_MATERIAL
sea_lantern_core|SEA_LANTERN|FISHING_MATERIAL
slime_core|SLIME_BALL|MONSTER_MATERIAL
magma_core|MAGMA_CREAM|MONSTER_MATERIAL
phantom_wing|PHANTOM_MEMBRANE|MONSTER_MATERIAL
rabbit_luck_foot|RABBIT_FOOT|MONSTER_MATERIAL
wolf_fang|BONE|MONSTER_MATERIAL
guardian_eye|ENDER_EYE|FISHING_MATERIAL
bee_wax_core|HONEYCOMB|FARMING_MATERIAL
honeyed_cloth|YELLOW_WOOL|FARMING_MATERIAL
cactus_spine_bundle|CACTUS|FARMING_MATERIAL
sugar_cane_fiber|PAPER|FARMING_MATERIAL
carrot_gold_mix|GOLDEN_CARROT|FARMING_MATERIAL
potato_starch|POTATO|FARMING_MATERIAL
beetroot_extract|BEETROOT|FARMING_MATERIAL
glow_berry_extract|GLOW_BERRIES|FARMING_MATERIAL
chorus_essence|POPPED_CHORUS_FRUIT|MAGIC_MATERIAL
obsidian_plate|OBSIDIAN|MINING_MATERIAL
crying_obsidian_shard|CRYING_OBSIDIAN|MINING_MATERIAL
blackstone_plate|POLISHED_BLACKSTONE|MINING_MATERIAL
ancient_debris_fragment|ANCIENT_DEBRIS|MINING_MATERIAL
netherite_thread|NETHERITE_SCRAP|RECIPE_MATERIAL
star_metal_dust|GLOWSTONE_DUST|MAGIC_MATERIAL
""";

    // id|name|base material|item type|collection|tier|ingredient identity and amount
    private static final String RECIPES="""
reinforced_iron_chestplate|Reinforced Iron Chestplate|IRON_CHESTPLATE|ARMOR|iron|4|c:heavy_iron_plate*16,c:enchanted_iron_ingot*32,c:reinforced_leather*16
deep_cave_pickaxe|Deep Cave Pickaxe|DIAMOND_PICKAXE|PICKAXE|cobblestone|5|c:compressed_cobblestone*64,c:deep_cave_crystal*8,m:STICK*2
diamond_edge_sword|Diamond Edge Sword|DIAMOND_SWORD|SWORD|diamond|4|c:diamond_edge_fragment*24,c:enchanted_diamond*16,m:STICK*1
emerald_guard_helmet|Emerald Guard Helmet|DIAMOND_HELMET|ARMOR|emerald|3|c:emerald_guard_gem*16,c:heavy_iron_plate*8
lapis_mana_wand|Lapis Mana Wand|BLAZE_ROD|WAND|lapis|4|c:lapis_mana_core*16,c:amethyst_focus_lens*4,m:STICK*1
redstone_power_bow|Redstone Power Bow|BOW|BOW|redstone|4|c:redstone_power_cell*8,c:hardened_string*24,m:STICK*3
quartz_focus_staff|Quartz Focus Staff|BLAZE_ROD|STAFF|quartz|3|c:quartz_focus_lens*16,m:BLAZE_ROD*1,c:mana_thread*8
amethyst_katana|Amethyst Katana|DIAMOND_SWORD|KATANA|amethyst|4|c:amethyst_focus_lens*16,c:diamond_edge_fragment*12,m:STICK*1
obsidian_guard_charm|Obsidian Guard Charm|OBSIDIAN|CHARM|obsidian|3|c:obsidian_plate*16,c:crying_obsidian_shard*8
netherite_thread_necklace|Netherite Thread Necklace|NETHERITE_SCRAP|NECKLACE|ancient_debris|4|c:netherite_thread*8,c:ancient_debris_fragment*16,m:NETHERITE_SCRAP*2
ancient_oak_axe|Ancient Oak Axe|DIAMOND_AXE|AXE|oak_log|4|c:ancient_oak_core*16,c:enchanted_oak_log*32,m:STICK*2
bamboo_shadow_dagger|Bamboo Shadow Dagger|IRON_SWORD|DAGGER|bamboo|3|c:refined_bamboo*32,c:shadow_dust*8,m:STICK*1
forest_guard_leggings|Forest Guard Leggings|LEATHER_LEGGINGS|ARMOR|oak_log|5|c:ancient_oak_core*24,c:reinforced_leather*24
leafrunner_boots|Leafrunner Boots|LEATHER_BOOTS|ARMOR|oak_leaves|3|c:reinforced_leather*16,c:enchanted_feather*8,m:SWEET_BERRIES*16
foragers_totem|Forager's Totem|TOTEM_OF_UNDYING|TOTEM|birch_log|4|c:ancient_oak_core*8,c:refined_bamboo*16,m:TOTEM_OF_UNDYING*1
woodland_ring|Woodland Ring|GOLD_NUGGET|RING|spruce_log|3|c:ancient_oak_core*8,m:GOLD_NUGGET*16
thornvine_charm|Thornvine Charm|VINE|CHARM|vine|3|c:cactus_spine_bundle*8,m:VINE*32,m:MOSS_BLOCK*16
jungle_hunter_bow|Jungle Hunter Bow|BOW|BOW|jungle_log|4|c:ancient_oak_core*16,c:hardened_string*32,c:enchanted_feather*8
mangrove_mud_staff|Mangrove Mud Staff|MANGROVE_PROPAGULE|STAFF|mangrove_log|3|c:ancient_oak_core*8,m:MUD*64,c:amethyst_focus_lens*4
cherry_blossom_katana|Cherry Blossom Katana|DIAMOND_SWORD|KATANA|cherry_log|4|c:ancient_oak_core*12,c:sakura_petals*32,m:STICK*1
golden_crop_hoe|Golden Crop Hoe|GOLDEN_HOE|FARMING_HOE|wheat|4|c:golden_crop_fiber*24,c:enchanted_wheat*32,m:GOLDEN_HOE*1
sugar_cane_slicer|Sugar Cane Slicer|IRON_HOE|FARMING_HOE|sugar_cane|4|c:sugar_cane_fiber*32,c:enchanted_sugar_cane*16,m:IRON_HOE*1
pumpkin_guard_chestplate|Pumpkin Guard Chestplate|LEATHER_CHESTPLATE|ARMOR|pumpkin|4|c:enchanted_pumpkin_chunk*32,c:reinforced_leather*24
melon_mender_food|Melon Mender Food|MELON_SLICE|FOOD|melon|3|c:enchanted_melon_chunk*8,m:MELON_SLICE*32,c:honey_glaze*4
carrot_gold_ring|Carrot Gold Ring|GOLDEN_CARROT|RING|carrot|3|c:carrot_gold_mix*16,m:GOLDEN_CARROT*8
potato_starch_bread|Potato Starch Bread|BREAD|FOOD|potato|2|c:potato_starch*16,m:BREAD*1
beetroot_vitality_potion|Beetroot Vitality Potion|POTION|POTION|beetroot|3|c:beetroot_extract*16,m:GLASS_BOTTLE*1
honeyed_farmer_gloves|Honeyed Farmer Gloves|LEATHER|FARMING_EQUIPMENT|honey_bottle|3|c:honeyed_cloth*24,m:HONEY_BOTTLE*16,c:reinforced_leather*8
glow_berry_mana_food|Glow Berry Mana Food|GLOW_BERRIES|FOOD|glow_berries|3|c:glow_berry_extract*16,c:mana_leaf*8
cactus_spine_dagger|Cactus Spine Dagger|IRON_SWORD|DAGGER|cactus|3|c:cactus_spine_bundle*24,c:venom_sac*8,m:STICK*1
bonebreaker_sword|Bonebreaker Sword|IRON_SWORD|SWORD|bone|4|c:sharp_bone_shard*32,c:enchanted_monster_bone*16,m:STICK*1
spider_silk_bow|Spider Silk Bow|BOW|BOW|string|4|c:spider_silk_bundle*32,c:hardened_string*16,m:STICK*3
undead_heart_charm|Undead Heart Charm|FERMENTED_SPIDER_EYE|CHARM|rotten_flesh|4|c:undead_heart*8,c:rotten_monster_flesh*32
venom_eye_wand|Venom Eye Wand|SPIDER_EYE|WAND|spider_eye|3|m:SPIDER_EYE*64,c:toxic_dust*16,m:STICK*1
slime_core_totem|Slime Core Totem|TOTEM_OF_UNDYING|TOTEM|slime_ball|3|c:slime_core*24,m:TOTEM_OF_UNDYING*1
magma_core_staff|Magma Core Staff|BLAZE_ROD|STAFF|magma_cream|4|c:magma_core*16,m:BLAZE_ROD*1,c:infernal_core*4
phantom_wing_boots|Phantom Wing Boots|LEATHER_BOOTS|ARMOR|phantom_membrane|3|c:phantom_wing*16,c:reinforced_leather*16
rabbit_luck_necklace|Rabbit Luck Necklace|RABBIT_FOOT|NECKLACE|rabbit_foot|3|c:rabbit_luck_foot*8,m:GOLD_NUGGET*16
wolf_fang_katana|Wolf Fang Katana|IRON_SWORD|KATANA|bone|5|c:wolf_fang*16,c:sharp_bone_shard*24,m:STICK*1
guardian_eye_ring|Guardian Eye Ring|ENDER_EYE|RING|prismarine_shard|4|c:guardian_eye*8,c:prismarine_guard_scale*16
prismarine_guard_helmet|Prismarine Guard Helmet|DIAMOND_HELMET|ARMOR|prismarine_shard|4|c:prismarine_guard_scale*24,c:dark_prismarine_shard*16
sea_lantern_wand|Sea Lantern Wand|SEA_LANTERN|WAND|sea_lantern|3|c:sea_lantern_core*16,m:PRISMARINE_CRYSTALS*32,m:STICK*1
deep_sea_necklace|Deep Sea Necklace|NAUTILUS_SHELL|NECKLACE|cod|4|c:silver_fish_scale*24,c:pearl_scale*16
storm_pearl_fishing_rod|Storm Pearl Fishing Rod|FISHING_ROD|FISHING_ROD|ender_pearl|3|c:storm_pearl*8,m:STRING*32,m:STICK*3
dried_kelp_survival_food|Dried Kelp Survival Food|DRIED_KELP|FOOD|kelp|2|c:dried_seaweed_bundle*16,m:DRIED_KELP*32
pufferfish_toxic_potion|Pufferfish Toxic Potion|POTION|POTION|pufferfish|3|m:PUFFERFISH*16,c:toxic_vial*8,m:GLASS_BOTTLE*1
salmon_hunter_charm|Salmon Hunter Charm|SALMON|CHARM|salmon|3|m:SALMON*64,c:fisherman_spice*16
nautilus_totem|Nautilus Totem|TOTEM_OF_UNDYING|TOTEM|nautilus_shell|4|m:NAUTILUS_SHELL*8,c:sea_lantern_core*8,m:TOTEM_OF_UNDYING*1
tropical_fish_ring|Tropical Fish Ring|TROPICAL_FISH|RING|tropical_fish|3|m:TROPICAL_FISH*32,c:pearl_scale*8
angler_boots|Angler Boots|LEATHER_BOOTS|ARMOR|cod|4|c:silver_fish_scale*16,c:reinforced_leather*16
blazing_powder_wand|Blazing Powder Wand|BLAZE_POWDER|WAND|blaze_rod|4|c:blazing_powder_core*16,m:BLAZE_ROD*1,c:sun_dust*8
void_pearl_dagger|Void Pearl Dagger|IRON_SWORD|DAGGER|ender_pearl|4|c:void_pearl_fragment*16,c:shadow_dust*12,m:STICK*1
chorus_staff|Chorus Staff|END_ROD|STAFF|chorus_fruit|3|c:chorus_essence*24,m:END_ROD*1,c:mana_thread*8
blackstone_guard_chestplate|Blackstone Guard Chestplate|NETHERITE_CHESTPLATE|ARMOR|blackstone|4|c:blackstone_plate*24,c:heavy_iron_plate*16
nether_brick_broadsword|Nether Brick Broadsword|IRON_SWORD|SWORD|nether_brick|3|c:nether_brick_core*16,m:BLAZE_ROD*1,m:STICK*1
end_stone_pickaxe|End Stone Pickaxe|DIAMOND_PICKAXE|PICKAXE|end_stone|4|c:end_stone_core*32,c:diamond_edge_fragment*8,m:STICK*2
crying_obsidian_totem|Crying Obsidian Totem|TOTEM_OF_UNDYING|TOTEM|crying_obsidian|3|c:crying_obsidian_shard*16,c:obsidian_plate*8,m:TOTEM_OF_UNDYING*1
star_metal_necklace|Star Metal Necklace|NETHERITE_INGOT|NECKLACE|netherite|5|c:star_metal_dust*16,c:netherite_thread*8,m:NETHERITE_INGOT*1
magma_fury_katana|Magma Fury Katana|NETHERITE_SWORD|KATANA|magma_cream|5|c:magma_core*24,c:infernal_core*8,m:STICK*1
void_guard_ring|Void Guard Ring|ENDER_EYE|RING|end_stone|5|c:void_pearl_fragment*16,c:end_stone_core*24
""";

    public static void apply(ConfigurationSection root){applyDataset(root,MATERIALS,RECIPES);AdditionalCollectionRecipeDefaults.apply(root);}
    public static Set<String> recipeIds(){LinkedHashSet<String>ids=new LinkedHashSet<>(ids(RECIPES));ids.addAll(AdditionalCollectionRecipeDefaults.recipeIds());return Set.copyOf(ids);}
    static void applyDataset(ConfigurationSection root,String materials,String recipes){applyMaterials(root,materials);for(String line:recipes.strip().split("\\R"))applyRecipe(root,line);}
    static Set<String> ids(String recipes){LinkedHashSet<String>ids=new LinkedHashSet<>();for(String line:recipes.strip().split("\\R")){String[]p=line.split("\\|",2);if(p.length>0)ids.add(norm(p[0]));}return ids;}

    private static void applyMaterials(ConfigurationSection root,String materials){for(String line:materials.strip().split("\\R")){String[]p=line.split("\\|");if(p.length<3)continue;String id=norm(p[0]),base="custom-items.items."+id;if(root.isConfigurationSection(base))continue;root.set(base+".name","&a"+pretty(id));root.set(base+".material",p[1]);root.set(base+".type","RECIPE_MATERIAL");root.set(base+".mmo.type","RECIPE_MATERIAL");root.set(base+".source-type",p[2]);root.set(base+".category","materials");root.set(base+".rarity","UNCOMMON");root.set(base+".lore",List.of("&7A stackable progression material."));root.set(base+".mmo.random-roll",false);root.set(base+".mmo.stats",null);root.set(base+".ability.type","none");root.set(base+".ability.mana-cost",0);root.set(base+".consumable.enabled",false);root.set(base+".max-stack-size",64);root.set(base+".guild-sellable",true);root.set(base+".sellall-allowed",true);}}

    private static void applyRecipe(ConfigurationSection root,String line){String[]p=line.split("\\|",7);if(p.length<7)return;String id=norm(p[0]),item="custom-items.items."+id,recipe="recipes."+id;if(!root.isConfigurationSection(item)){root.set(item+".name","&a"+p[1]);root.set(item+".material",p[2]);root.set(item+".type",p[3]);root.set(item+".mmo.type",p[3]);root.set(item+".category",category(p[3]).toLowerCase(Locale.ROOT));root.set(item+".rarity","RARE");root.set(item+".lore",List.of("&7Crafted through collection progression."));root.set(item+".mmo.random-roll",false);root.set(item+".ability.type","none");root.set(item+".ability.mana-cost",0);root.set(item+".unbreakable",true);applyConsumable(root,item,id);}
        if(root.isConfigurationSection(recipe))return;String collection=norm(p[4]);int tier=Integer.parseInt(p[5]);root.set(recipe+".display-name","&a"+p[1]);root.set(recipe+".category",category(p[3]));root.set(recipe+".result",id);root.set(recipe+".unlock.type","COLLECTION");root.set(recipe+".unlock.collection",collection);root.set(recipe+".unlock.tier",tier);String[]parts=p[6].split(",");StringBuilder shape=new StringBuilder();for(int i=0;i<parts.length&&i<3;i++){char symbol=(char)('A'+i);String[]amount=parts[i].split("\\*",2);String value=amount[0];root.set(recipe+".ingredients."+symbol,(value.startsWith("c:")?"custom:":"material:")+value.substring(2));root.set(recipe+".amounts."+symbol,Math.clamp(Integer.parseInt(amount[1]),1,64));shape.append(symbol);}while(shape.length()<3)shape.append(' ');root.set(recipe+".shape",List.of(shape.toString(),"   ","   "));
        String unlock="collections.items."+collection+".unlocks."+tier;if(!root.isConfigurationSection(unlock)){root.set(unlock+".amount",tierAmount(collection,tier));root.set(unlock+".reward-type","recipe");root.set(unlock+".reward-value",id);root.set(unlock+".name","&a"+p[1]);}String tierPath="collections."+collectionCategory(collection)+"."+collection+".tiers."+tier;root.set(tierPath+".required",tierAmount(collection,tier));List<String>rewards=new ArrayList<>(root.getStringList(tierPath+".rewards"));String reward="recipe:"+id;if(!rewards.contains(reward))rewards.add(reward);root.set(tierPath+".rewards",rewards);}

    private static void applyConsumable(ConfigurationSection root,String item,String id){if(id.equals("melon_mender_food")){root.set(item+".consumable.enabled",true);root.set(item+".consumable.heal",80);}else if(id.equals("melon_juice_food")){root.set(item+".consumable.enabled",true);root.set(item+".consumable.heal",100);}else if(id.equals("cod_oil_food")){root.set(item+".consumable.enabled",true);root.set(item+".consumable.heal",60);}else if(id.equals("potato_starch_bread")){root.set(item+".consumable.enabled",true);root.set(item+".consumable.heal",35);}else if(id.equals("dried_kelp_survival_food")){root.set(item+".consumable.enabled",true);root.set(item+".consumable.heal",25);root.set(item+".consumable.food",6);}else if(id.equals("glow_berry_mana_food")){root.set(item+".consumable.enabled",true);root.set(item+".consumable.mana",60);}else if(id.equals("beetroot_vitality_potion")){root.set(item+".consumable.enabled",true);root.set(item+".consumable.stat-buffs.HEALTH.amount",50);root.set(item+".consumable.stat-buffs.HEALTH.duration-seconds",180);}else if(id.equals("beetroot_life_potion")){root.set(item+".consumable.enabled",true);root.set(item+".consumable.stat-buffs.HEALTH.amount",75);root.set(item+".consumable.stat-buffs.HEALTH.duration-seconds",180);}else if(id.equals("pufferfish_toxic_potion")){root.set(item+".consumable.enabled",true);root.set(item+".consumable.stat-buffs.STRENGTH.amount",15);root.set(item+".consumable.stat-buffs.STRENGTH.duration-seconds",120);}}
    private static int tierAmount(String collection,int tier){boolean rare=Set.of("diamond","emerald","ancient_debris","netherite").contains(collection);int[]values=rare?new int[]{10,25,50,100,250,500,1000,2500,5000,10000}:new int[]{50,100,250,500,1000,2500,5000,10000,25000,50000};return values[Math.clamp(tier-1,0,9)];}
    private static String collectionCategory(String id){if(Set.of("wheat","sugar_cane","melon","pumpkin","beetroot","carrot","potato","honey_bottle","cactus","glow_berries").contains(id))return"farming";if(Set.of("oak_log","spruce_log","birch_log","jungle_log","mangrove_log","cherry_log","oak_leaves","bamboo","vine").contains(id))return"foraging";if(Set.of("cod","salmon","pufferfish","tropical_fish","kelp","nautilus_shell","sea_lantern","prismarine_shard","prismarine_crystals").contains(id))return"fishing";if(Set.of("bone","string","spider_eye","ender_pearl","slime_ball","magma_cream","ghast_tear","phantom_membrane","rabbit_foot","blaze_rod","rotten_flesh").contains(id))return"combat";return"mining";}
    private static String category(String type){return switch(type){case"ARMOR"->"ARMOR";case"PICKAXE","AXE"->"TOOLS";case"FARMING_HOE","FARMING_EQUIPMENT"->"FARMING_EQUIPMENT";case"FISHING_ROD"->"FISHING_EQUIPMENT";case"FOOD"->"FOOD";case"POTION"->"POTIONS";case"WAND","STAFF"->"MAGIC";case"CHARM","TOTEM","NECKLACE","RING"->"ACCESSORIES";default->"WEAPONS";};}
    private static String norm(String value){return value.toLowerCase(Locale.ROOT).replace('-','_').replace(' ','_');}
    private static String pretty(String value){StringBuilder out=new StringBuilder();for(String part:norm(value).split("_")){if(!out.isEmpty())out.append(' ');out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));}return out.toString();}
}
