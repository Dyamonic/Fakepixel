package club.abelix.games.fakevival.config.defaults;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

/** Third guarded Java-only batch: one vanilla path, inert core, and recipe per row. */
public final class MissingProgressionDefaults100B {
    private MissingProgressionDefaults100B() {}

    // collection id|tracked Bukkit material|category|tier|rare|output Bukkit material|output type
    private static final String PATHS = """
dirt|DIRT|mining|3|false|IRON_SHOVEL|SHOVEL
grass_block|GRASS_BLOCK|mining|3|false|IRON_SHOVEL|SHOVEL
coarse_dirt|COARSE_DIRT|mining|3|false|IRON_BOOTS|ARMOR
podzol|PODZOL|foraging|3|false|LEATHER_BOOTS|ARMOR
mycelium|MYCELIUM|foraging|4|false|BROWN_MUSHROOM|CHARM
grassy_dirt|DIRT_PATH|mining|3|false|IRON_SHOVEL|SHOVEL
farmland|FARMLAND|farming|4|false|DIAMOND_HOE|FARMING_HOE
packed_mud|PACKED_MUD|mining|4|false|IRON_CHESTPLATE|ARMOR
mud_bricks|MUD_BRICKS|mining|4|false|IRON_LEGGINGS|ARMOR
mossy_cobblestone|MOSSY_COBBLESTONE|mining|4|false|IRON_SWORD|SWORD
mossy_stone_bricks|MOSSY_STONE_BRICKS|mining|4|false|IRON_CHESTPLATE|ARMOR
cracked_stone_bricks|CRACKED_STONE_BRICKS|mining|4|false|IRON_SWORD|SWORD
chiseled_stone_bricks|CHISELED_STONE_BRICKS|mining|4|false|STONE|CHARM
polished_granite|POLISHED_GRANITE|mining|4|false|IRON_PICKAXE|PICKAXE
polished_diorite|POLISHED_DIORITE|mining|4|false|BLAZE_ROD|WAND
polished_andesite|POLISHED_ANDESITE|mining|4|false|IRON_BOOTS|ARMOR
polished_tuff|POLISHED_TUFF|mining|4|false|IRON_PICKAXE|PICKAXE
tuff_bricks|TUFF_BRICKS|mining|4|false|IRON_CHESTPLATE|ARMOR
chiseled_tuff|CHISELED_TUFF|mining|4|false|GOLD_NUGGET|RING
chiseled_tuff_bricks|CHISELED_TUFF_BRICKS|mining|5|false|DIAMOND_PICKAXE|PICKAXE
oak_planks|OAK_PLANKS|foraging|3|false|IRON_AXE|AXE
spruce_planks|SPRUCE_PLANKS|foraging|3|false|BOW|BOW
birch_planks|BIRCH_PLANKS|foraging|3|false|IRON_SWORD|DAGGER
jungle_planks|JUNGLE_PLANKS|foraging|3|false|BOW|BOW
acacia_planks|ACACIA_PLANKS|foraging|3|false|IRON_AXE|AXE
dark_oak_planks|DARK_OAK_PLANKS|foraging|4|false|IRON_SWORD|SWORD
mangrove_planks|MANGROVE_PLANKS|foraging|4|false|BLAZE_ROD|STAFF
cherry_planks|CHERRY_PLANKS|foraging|4|false|DIAMOND_SWORD|KATANA
pale_oak_planks|PALE_OAK_PLANKS|foraging|4|false|BONE|CHARM
bamboo_planks|BAMBOO_PLANKS|foraging|4|false|IRON_SWORD|KATANA
crimson_planks|CRIMSON_PLANKS|foraging|4|false|NETHERITE_AXE|AXE
warped_planks|WARPED_PLANKS|foraging|4|false|END_ROD|STAFF
oak_boat|OAK_BOAT|tools|3|false|COMPASS|CHARM
cherry_boat|CHERRY_BOAT|tools|3|false|PINK_PETALS|CHARM
mangrove_boat|MANGROVE_BOAT|tools|3|false|MANGROVE_PROPAGULE|CHARM
seagrass|SEAGRASS|fishing|3|false|PRISMARINE_SHARD|CHARM
sea_pickle|SEA_PICKLE|fishing|3|false|SEA_LANTERN|WAND
lily_pad|LILY_PAD|foraging|3|false|GOLD_NUGGET|RING
tube_coral|TUBE_CORAL|fishing|3|false|PRISMARINE_CRYSTALS|CHARM
brain_coral|BRAIN_CORAL|fishing|3|false|AMETHYST_SHARD|RING
bubble_coral|BUBBLE_CORAL|fishing|3|false|BLAZE_ROD|WAND
fire_coral|FIRE_CORAL|fishing|4|false|IRON_SWORD|SWORD
horn_coral|HORN_CORAL|fishing|4|false|TRIDENT|SWORD
tube_coral_block|TUBE_CORAL_BLOCK|fishing|4|false|DIAMOND_HELMET|ARMOR
brain_coral_block|BRAIN_CORAL_BLOCK|fishing|4|false|DIAMOND_CHESTPLATE|ARMOR
bubble_coral_block|BUBBLE_CORAL_BLOCK|fishing|4|false|DIAMOND_BOOTS|ARMOR
fire_coral_block|FIRE_CORAL_BLOCK|fishing|4|false|DIAMOND_SWORD|SWORD
horn_coral_block|HORN_CORAL_BLOCK|fishing|4|false|TRIDENT|SWORD
conduit|CONDUIT|fishing|6|true|HEART_OF_THE_SEA|TOTEM
sponge|SPONGE|fishing|5|true|DIAMOND_HELMET|ARMOR
wet_sponge|WET_SPONGE|fishing|5|true|DIAMOND_CHESTPLATE|ARMOR
prismarine|PRISMARINE|fishing|4|false|DIAMOND_LEGGINGS|ARMOR
prismarine_bricks|PRISMARINE_BRICKS|fishing|5|false|DIAMOND_CHESTPLATE|ARMOR
dark_prismarine|DARK_PRISMARINE|fishing|5|false|DIAMOND_SWORD|SWORD
obsidian|OBSIDIAN|mining|5|false|DIAMOND_CHESTPLATE|ARMOR
crying_obsidian|CRYING_OBSIDIAN|mining|5|true|TOTEM_OF_UNDYING|TOTEM
magma_block|MAGMA_BLOCK|combat|4|false|DIAMOND_BOOTS|ARMOR
nether_bricks|NETHER_BRICKS|mining|4|false|IRON_CHESTPLATE|ARMOR
red_nether_bricks|RED_NETHER_BRICKS|mining|4|false|IRON_SWORD|SWORD
nether_sprouts|NETHER_SPROUTS|foraging|3|false|WARPED_FUNGUS|CHARM
weeping_vines|WEEPING_VINES|foraging|3|false|IRON_SWORD|SWORD
twisting_vines|TWISTING_VINES|foraging|3|false|BLAZE_ROD|WAND
crimson_roots|CRIMSON_ROOTS|foraging|3|false|RED_DYE|CHARM
warped_roots|WARPED_ROOTS|foraging|3|false|CYAN_DYE|CHARM
nether_gold_ore|NETHER_GOLD_ORE|mining|4|false|GOLDEN_PICKAXE|PICKAXE
nether_quartz_ore|NETHER_QUARTZ_ORE|mining|4|false|BLAZE_ROD|WAND
netherite_scrap|NETHERITE_SCRAP|mining|5|true|NETHERITE_SWORD|SWORD
end_stone_bricks|END_STONE_BRICKS|mining|5|false|DIAMOND_CHESTPLATE|ARMOR
chorus_plant|CHORUS_PLANT|foraging|4|false|END_ROD|STAFF
chorus_flower|CHORUS_FLOWER|foraging|4|false|PURPUR_BLOCK|RING
popped_chorus_fruit|POPPED_CHORUS_FRUIT|farming|4|false|ENDER_EYE|CHARM
end_rod|END_ROD|magic|5|false|BLAZE_ROD|STAFF
purpur_pillar|PURPUR_PILLAR|mining|4|false|DIAMOND_LEGGINGS|ARMOR
shulker_shell|SHULKER_SHELL|combat|5|true|SHULKER_BOX|CHARM
dragon_egg|DRAGON_EGG|combat|7|true|DRAGON_HEAD|TOTEM
crafting_table|CRAFTING_TABLE|tools|3|false|IRON_PICKAXE|MULTI_TOOL
furnace|FURNACE|tools|3|false|IRON_CHESTPLATE|ARMOR
blast_furnace|BLAST_FURNACE|tools|4|false|IRON_PICKAXE|DRILL
smoker|SMOKER|tools|4|false|COOKED_BEEF|CHARM
stonecutter|STONECUTTER|tools|4|false|IRON_SWORD|DAGGER
grindstone|GRINDSTONE|tools|4|false|IRON_SWORD|SWORD
smithing_table|SMITHING_TABLE|tools|5|false|NETHERITE_PICKAXE|MULTI_TOOL
fletching_table|FLETCHING_TABLE|tools|4|false|CROSSBOW|CROSSBOW
cartography_table|CARTOGRAPHY_TABLE|tools|4|false|MAP|CHARM
loom|LOOM|tools|4|false|WHITE_WOOL|CHARM
anvil|ANVIL|tools|5|false|IRON_CHESTPLATE|ARMOR
chipped_anvil|CHIPPED_ANVIL|tools|4|false|IRON_SWORD|SWORD
damaged_anvil|DAMAGED_ANVIL|tools|4|false|IRON_PICKAXE|PICKAXE
brewing_stand|BREWING_STAND|magic|5|false|POTION|WAND
cauldron|CAULDRON|magic|4|false|POTION|CHARM
enchanting_table|ENCHANTING_TABLE|magic|6|true|ENCHANTED_BOOK|STAFF
ender_chest|ENDER_CHEST|magic|6|true|ENDER_EYE|TOTEM
barrel|BARREL|tools|3|false|CHEST|CHARM
chest|CHEST|tools|3|false|ENDER_CHEST|CHARM
trapped_chest|TRAPPED_CHEST|tools|4|false|REDSTONE_TORCH|CHARM
jukebox|JUKEBOX|tools|4|false|MUSIC_DISC_13|CHARM
note_block|NOTE_BLOCK|tools|4|false|GOAT_HORN|CHARM
beacon|BEACON|magic|7|true|NETHER_STAR|TOTEM
respawn_anchor|RESPAWN_ANCHOR|magic|6|true|CRYING_OBSIDIAN|TOTEM
lodestone|LODESTONE|tools|6|true|COMPASS|NECKLACE
""";

    private static final int[] COMMON={50,100,250,500,1000,2500,5000,10000,25000,50000};
    private static final int[] RARE={1,5,10,25,50,100,250,500,1000,2500};

    public static void apply(ConfigurationSection root){for(Path p:paths()){material(root,p);collection(root,p);recipe(root,p);}}
    public static Set<String> recipeIds(){Set<String>s=new LinkedHashSet<>();for(Path p:paths())s.add(outputId(p));return Set.copyOf(s);}
    public static Set<String> materialIds(){Set<String>s=new LinkedHashSet<>();for(Path p:paths())s.add(coreId(p));return Set.copyOf(s);}
    public static Set<String> collectionIds(){Set<String>s=new LinkedHashSet<>();for(Path p:paths())s.add(p.id());return Set.copyOf(s);}

    private static List<Path> paths(){List<Path>out=new ArrayList<>();for(String line:PATHS.strip().split("\\R")){String[]p=line.split("\\|");if(p.length==7)out.add(new Path(n(p[0]),p[1],p[2],Integer.parseInt(p[3]),Boolean.parseBoolean(p[4]),p[5],p[6]));}return List.copyOf(out);}
    private static void material(ConfigurationSection root,Path p){String b="custom-items.items."+coreId(p);if(root.isConfigurationSection(b))return;root.set(b+".name","&f"+pretty(p.id())+" Progression Core");root.set(b+".material",safeItemMaterial(p));root.set(b+".type","RECIPE_MATERIAL");root.set(b+".mmo.type","RECIPE_MATERIAL");root.set(b+".category",materialCategory(p.category()));root.set(b+".collection.category",p.category());root.set(b+".recipe-material.category",p.category());root.set(b+".rarity","COMMON");root.set(b+".random-rarity",false);root.set(b+".mmo.random-roll",false);root.set(b+".mmo.stats",null);root.set(b+".lore",List.of("&7A deterministic vanilla progression material."));root.set(b+".max-stack-size",64);root.set(b+".ability.type","none");root.set(b+".ability.mana-cost",0);root.set(b+".consumable.enabled",false);root.set(b+".unbreakable",false);root.set(b+".tradeable",true);root.set(b+".soulbound",false);root.set(b+".guild-sellable",true);root.set(b+".sellall-allowed",true);}
    private static void collection(ConfigurationSection root,Path p){String b="collections.items."+p.id();if(!root.isConfigurationSection(b)){root.set(b+".name","&e"+pretty(p.id())+" Collection");root.set(b+".category",p.category());root.set(b+".icon",safeItemMaterial(p));root.set(b+".materials",List.of(p.material()));}else{List<String>m=new ArrayList<>(root.getStringList(b+".materials"));if(m.stream().noneMatch(p.material()::equalsIgnoreCase))m.add(p.material());root.set(b+".materials",m);}int[]a=p.rare()?RARE:COMMON;for(int t=1;t<=10;t++){String q="collections."+p.category()+"."+p.id()+".tiers."+t;if(!root.contains(q+".required"))root.set(q+".required",a[t-1]);if(!root.contains(q+".rewards")||root.getStringList(q+".rewards").isEmpty())root.set(q+".rewards",List.of("perk:tier_"+t));}}
    private static void recipe(ConfigurationSection root,Path p){String id=outputId(p),item="custom-items.items."+id,r="recipes."+id;if(!root.isConfigurationSection(item)){root.set(item+".name","&a"+pretty(p.id())+" Masterwork");root.set(item+".material",p.outputMaterial());root.set(item+".type",p.type());root.set(item+".mmo.type",p.type());root.set(item+".category",outputCategory(p.type()));root.set(item+".rarity",p.rare()?"EPIC":"RARE");root.set(item+".random-rarity",false);root.set(item+".mmo.random-roll",false);root.set(item+".ability.type","none");root.set(item+".ability.mana-cost",0);root.set(item+".unbreakable",true);}if(!root.isConfigurationSection(r)){root.set(r+".display-name","&a"+pretty(p.id())+" Masterwork");root.set(r+".category",recipeCategory(p.type()));root.set(r+".result",id);root.set(r+".unlock.type","COLLECTION");root.set(r+".unlock.collection",p.id());root.set(r+".unlock.tier",p.tier());root.set(r+".ingredients.A","custom:"+coreId(p));root.set(r+".amounts.A",24);root.set(r+".ingredients.B","material:"+safeItemMaterial(p));root.set(r+".amounts.B",8);root.set(r+".shape",List.of("AB ","   ","   "));}String q="collections."+p.category()+"."+p.id()+".tiers."+p.tier();List<String>rewards=new ArrayList<>(root.getStringList(q+".rewards"));String reward="recipe:"+id;if(!rewards.contains(reward))rewards.add(reward);root.set(q+".rewards",rewards);}
    private static String coreId(Path p){return p.id()+"_progression_core";}private static String outputId(Path p){return p.id()+"_masterwork";}
    private static String safeItemMaterial(Path p){Material material=Material.matchMaterial(p.material());if(material!=null&&material.isItem())return material.name();return switch(p.category()){case"farming"->"WHEAT";case"foraging"->"VINE";case"combat"->"ROTTEN_FLESH";case"fishing"->"PRISMARINE_SHARD";case"magic"->"AMETHYST_SHARD";case"tools"->"IRON_INGOT";default->"COBBLESTONE";};}
    private static String materialCategory(String c){return switch(c){case"farming"->"farming_materials";case"foraging"->"foraging_materials";case"combat"->"combat_materials";case"fishing"->"fishing_materials";case"magic"->"magic_materials";default->"mining_materials";};}
    private static String outputCategory(String t){return switch(t){case"ARMOR"->"armor";case"PICKAXE","SHOVEL","AXE","DRILL","MULTI_TOOL","FARMING_HOE"->"tools";case"CHARM","RING","NECKLACE","TOTEM"->"accessories";case"WAND","STAFF"->"magic";default->"weapons";};}
    private static String recipeCategory(String t){return switch(t){case"ARMOR"->"ARMOR";case"PICKAXE","SHOVEL","AXE","DRILL","MULTI_TOOL","FARMING_HOE"->"TOOLS";case"CHARM","RING","NECKLACE","TOTEM"->"ACCESSORIES";case"WAND","STAFF"->"MAGIC";default->"WEAPONS";};}
    private static String n(String v){return v.toLowerCase(Locale.ROOT).replace('-','_').replace(' ','_');}private static String pretty(String v){StringBuilder b=new StringBuilder();for(String p:n(v).split("_")){if(!b.isEmpty())b.append(' ');b.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));}return b.toString();}
    private record Path(String id,String material,String category,int tier,boolean rare,String outputMaterial,String type){}
}
