package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;
import java.util.List;
import java.util.Locale;

public final class CookingDefaults {
 private CookingDefaults(){}
 private static final String INGREDIENTS="""
flour|SUGAR
dough|CLAY_BALL
sweet_dough|CLAY_BALL
bread_dough|CLAY_BALL
cake_batter|MUSHROOM_STEW
cookie_dough|COCOA_BEANS
pie_crust|PUMPKIN_SEEDS
butter|GOLD_NUGGET
cheese|YELLOW_DYE
cream|WHITE_DYE
milk_bucket_custom|MILK_BUCKET
sugar_powder|SUGAR
salt|QUARTZ
pepper|GUNPOWDER
spice_mix|BROWN_DYE
hot_spice_mix|BLAZE_POWDER
honey_glaze|HONEY_BOTTLE
chocolate_mix|COCOA_BEANS
berry_jam|SWEET_BERRIES
apple_slices|APPLE
carrot_slices|CARROT
potato_slices|POTATO
potato_starch|SUGAR
beetroot_slices|BEETROOT
beetroot_extract|RED_DYE
pumpkin_puree|PUMPKIN
melon_chunks|MELON_SLICE
melon_juice|POTION
fish_fillet|COD
salmon_fillet|SALMON
pufferfish_spice|PUFFERFISH
seaweed_wrap|DRIED_KELP
mushroom_slice|BROWN_MUSHROOM
cooked_mushroom_mix|MUSHROOM_STEW
egg_mix|EGG
golden_sauce|HONEY_BOTTLE
mana_syrup|HONEY_BOTTLE
healing_herbs|FERN
river_mint|LILY_PAD
fern_leaf|FERN
aloe_leaf|SMALL_DRIPLEAF
nether_wart_spice|NETHER_WART
frosting|WHITE_DYE
raw_beef_slice|BEEF
raw_pork_slice|PORKCHOP
raw_chicken_slice|CHICKEN
raw_mutton_slice|MUTTON
raw_rabbit_slice|RABBIT
cooked_meat_slice|COOKED_BEEF
cooked_fish_fillet|COOKED_COD
grilled_salmon_fillet|COOKED_SALMON
enchanted_flour|GLOWSTONE_DUST
enchanted_dough|SLIME_BALL
enchanted_butter|GOLD_INGOT
enchanted_cheese|GOLD_BLOCK
""";
 private static final String FOODS="""
roasted_carrot_slices|CARROT|25
baked_potato_slices|BAKED_POTATO|30
mushroom_stew_custom|MUSHROOM_STEW|60
fish_sandwich|BREAD|75
salmon_wrap|DRIED_KELP|85
spicy_fish_stew|RABBIT_STEW|90
honey_bread|BREAD|50
cheese_bread|BREAD|45
mana_cookie|COOKIE|0
pumpkin_pie_custom|PUMPKIN_PIE|75
berry_pie|PUMPKIN_PIE|65
healing_soup|BEETROOT_SOUP|120
""";
 public static void apply(ConfigurationSection root){station(root,"cutting_board","&aCutting Board","OAK_PRESSURE_PLATE","COOKING_STATION",List.of("&7A simple board for preparing ingredients.","&7Place it down and use a Kitchen Knife."));station(root,"kitchen_knife","&fKitchen Knife","IRON_SWORD","COOKING_TOOL",List.of("&7A cooking tool used with Cutting Boards.","&7Not meant for combat."));toolRecipe(root,"cutting_board",List.of("PPP","PBP"),'P',"OAK_PLANKS",'B',"OAK_PRESSURE_PLATE");toolRecipe(root,"kitchen_knife",List.of(" I "," I "," S "),'I',"IRON_INGOT",'S',"STICK");for(String line:INGREDIENTS.lines().filter(s->!s.isBlank()).toList()){String[]p=line.split("\\|");station(root,p[0],"&f"+pretty(p[0]),p[1],"COOKING_INGREDIENT",List.of("&7A prepared cooking ingredient."));}for(String line:FOODS.lines().filter(s->!s.isBlank()).toList()){String[]p=line.split("\\|");station(root,p[0],"&a"+pretty(p[0]),p[1],"FOOD",List.of("&7Prepared through the cooking system.","&7Restores &c"+p[2]+" MMO Health&7."));root.set("custom-items.items."+p[0]+".food.heal",Integer.parseInt(p[2]));}}
 private static void toolRecipe(ConfigurationSection root,String id,List<String>shape,Object...ingredients){String path="recipes."+id;root.set(path+".display-name",root.getString("custom-items.items."+id+".name","&f"+pretty(id)));root.set(path+".category","UTILITY");root.set(path+".result",id);root.set(path+".unlock.type","DEFAULT");root.set(path+".shape",shape);for(int i=0;i<ingredients.length;i+=2)root.set(path+".ingredients."+ingredients[i],ingredients[i+1]);}
 private static void station(ConfigurationSection root,String id,String name,String material,String type,List<String> lore){String b="custom-items.items."+id;if(!root.isConfigurationSection(b)){root.set(b+".name",name);root.set(b+".material",material);}root.set(b+".type",type);root.set(b+".mmo.type","MATERIAL");root.set(b+".category",type.equals("COOKING_INGREDIENT")?"cooking_ingredients":"cooking");root.set(b+".rarity","COMMON");root.set(b+".lore",lore);root.set(b+".special-item",false);root.set(b+".developer-notebook",false);root.set(b+".random-stats-allowed",false);root.set(b+".random-rarity-allowed",false);root.set(b+".fixed-rarity",true);root.set(b+".reforge-allowed",false);root.set(b+".enchant-allowed",false);root.set(b+".mmo.random-roll",false);root.set(b+".mmo.stats",null);root.set(b+".ability.type","none");root.set(b+".ability.mana-cost",0);root.set(b+".random-rarity",false);root.set(b+".max-stack-size",type.equals("COOKING_INGREDIENT")?64:1);root.set(b+".tradeable",true);root.set(b+".sellall-allowed",true);if(id.equals("cutting_board")){root.set(b+".cooking-station","CUTTING_BOARD");root.set(b+".placeable-custom-block",true);}if(id.equals("kitchen_knife")){root.set(b+".cooking-tool","KITCHEN_KNIFE");root.set(b+".knife-max-uses",128);root.set(b+".hide-attributes",true);root.set(b+".combat-damage-disabled",true);}}
 private static String pretty(String id){StringBuilder b=new StringBuilder();for(String p:id.split("_")){if(!b.isEmpty())b.append(' ');b.append(p.substring(0,1).toUpperCase(Locale.ROOT)).append(p.substring(1));}return b.toString();}
}
