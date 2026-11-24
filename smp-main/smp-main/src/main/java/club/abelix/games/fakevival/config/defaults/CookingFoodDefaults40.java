package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;
import java.util.*;

public final class CookingFoodDefaults40 {
 private CookingFoodDefaults40(){}
 private static final String DATA="""
campfire_toast|&fCampfire Toast|BREAD|COMMON|CAMPFIRE,SOUL_CAMPFIRE|30|0|c:bread_dough*1
buttered_toast|&eButtered Toast|BREAD|COMMON|CAMPFIRE,SOUL_CAMPFIRE,FURNACE|45|0|c:campfire_toast*1,c:butter*1
cheesy_toast|&eCheesy Toast|BREAD|UNCOMMON|CAMPFIRE,FURNACE,SMOKER|55|0|c:campfire_toast*1,c:cheese*1
garlic_flatbread|&aGarlic Flatbread|BREAD|UNCOMMON|FURNACE,SMOKER|65|0|c:bread_dough*1,c:spice_mix*1,c:butter*1
honey_glazed_bread|&6Honey Glazed Bread|BREAD|UNCOMMON|CAMPFIRE,FURNACE|70|0|c:bread_dough*1,c:honey_glaze*1
sweet_berry_tart|&dSweet Berry Tart|PUMPKIN_PIE|UNCOMMON|FURNACE|75|0|c:pie_crust*1,c:berry_jam*2,c:sugar_powder*1
apple_hand_pie|&aApple Hand Pie|PUMPKIN_PIE|UNCOMMON|FURNACE|80|0|c:pie_crust*1,c:apple_slices*3,c:sugar_powder*1
pumpkin_spice_pie|&6Pumpkin Spice Pie|PUMPKIN_PIE|RARE|FURNACE|95|0|c:pie_crust*1,c:pumpkin_puree*3,c:spice_mix*1,c:sugar_powder*1
melon_cream_slice|&aMelon Cream Slice|MELON_SLICE|UNCOMMON|FURNACE|70|0|c:melon_chunks*3,c:cream*1,c:sugar_powder*1
chocolate_cookie|&6Chocolate Cookie|COOKIE|COMMON|FURNACE|35|0|c:cookie_dough*1,c:chocolate_mix*1
frosted_cookie|&fFrosted Cookie|COOKIE|UNCOMMON|FURNACE|45|0|c:cookie_dough*1,c:frosting*1
mana_frosted_cookie|&bMana Frosted Cookie|COOKIE|RARE|FURNACE|0|85|c:cookie_dough*1,c:frosting*1,c:mana_syrup*1
campfire_fish_skewer|&bCampfire Fish Skewer|COD|COMMON|CAMPFIRE,SOUL_CAMPFIRE|55|0|c:fish_fillet*2,m:STICK*1,c:salt*1
smoked_fish_skewer|&bSmoked Fish Skewer|COOKED_COD|UNCOMMON|SMOKER,CAMPFIRE|75|0|c:fish_fillet*2,c:spice_mix*1,m:STICK*1
spicy_salmon_skewer|&cSpicy Salmon Skewer|COOKED_SALMON|RARE|SMOKER,CAMPFIRE|95|0|c:salmon_fillet*2,c:hot_spice_mix*1,m:STICK*1
seaweed_fish_roll|&2Seaweed Fish Roll|KELP|UNCOMMON|CAMPFIRE,SMOKER|80|0|c:seaweed_wrap*1,c:cooked_fish_fillet*2
salmon_seaweed_roll|&cSalmon Seaweed Roll|KELP|RARE|CAMPFIRE,SMOKER|105|0|c:seaweed_wrap*1,c:grilled_salmon_fillet*2,c:spice_mix*1
puffer_spice_soup|&ePuffer Spice Soup|RABBIT_STEW|RARE|CAMPFIRE,SOUL_CAMPFIRE,FURNACE|90|0|c:pufferfish_spice*1,c:cooked_mushroom_mix*1,c:cream*1
fishermans_chowder|&bFisherman's Chowder|RABBIT_STEW|RARE|CAMPFIRE,FURNACE|125|0|c:cooked_fish_fillet*2,c:potato_slices*2,c:cream*1,c:salt*1
royal_seafood_stew|&6Royal Seafood Stew|RABBIT_STEW|EPIC|CAMPFIRE,FURNACE|180|40|c:grilled_salmon_fillet*2,c:cooked_fish_fillet*2,c:seaweed_wrap*1,c:hot_spice_mix*1
campfire_beef_bites|&fCampfire Beef Bites|COOKED_BEEF|COMMON|CAMPFIRE,SOUL_CAMPFIRE,SMOKER|60|0|c:raw_beef_slice*2,c:salt*1
smoked_pork_slices|&fSmoked Pork Slices|COOKED_PORKCHOP|COMMON|SMOKER,CAMPFIRE|65|0|c:raw_pork_slice*2,c:spice_mix*1
roasted_chicken_strips|&fRoasted Chicken Strips|COOKED_CHICKEN|COMMON|SMOKER,FURNACE|55|0|c:raw_chicken_slice*2,c:pepper*1
mutton_kebab|&aMutton Kebab|COOKED_MUTTON|UNCOMMON|CAMPFIRE,SMOKER|75|0|c:raw_mutton_slice*2,m:STICK*1,c:spice_mix*1
rabbit_roast|&aRabbit Roast|COOKED_RABBIT|UNCOMMON|CAMPFIRE,SMOKER|85|0|c:raw_rabbit_slice*2,c:carrot_slices*2,c:salt*1
spicy_meat_plate|&cSpicy Meat Plate|COOKED_BEEF|RARE|SMOKER,BLAST_FURNACE|120|0|c:cooked_meat_slice*3,c:hot_spice_mix*1
golden_sauce_steak|&6Golden Sauce Steak|COOKED_BEEF|EPIC|SMOKER,BLAST_FURNACE|175|0|c:cooked_meat_slice*3,c:golden_sauce*1,c:hot_spice_mix*1
roasted_carrot_bowl|&6Roasted Carrot Bowl|RABBIT_STEW|COMMON|CAMPFIRE,FURNACE|50|0|c:carrot_slices*3,c:butter*1,c:salt*1
loaded_potato_bowl|&eLoaded Potato Bowl|BAKED_POTATO|UNCOMMON|CAMPFIRE,FURNACE|80|0|c:potato_slices*3,c:cheese*1,c:butter*1
beetroot_cream_soup|&cBeetroot Cream Soup|BEETROOT_SOUP|UNCOMMON|CAMPFIRE,FURNACE|75|0|c:beetroot_slices*3,c:cream*1,c:salt*1
vegetable_stew|&aVegetable Stew|RABBIT_STEW|UNCOMMON|CAMPFIRE,FURNACE|95|0|c:carrot_slices*2,c:potato_slices*2,c:beetroot_slices*2,c:mushroom_slice*1
hearty_garden_stew|&aHearty Garden Stew|RABBIT_STEW|RARE|CAMPFIRE,FURNACE|145|0|c:vegetable_stew*1,c:healing_herbs*1,c:honey_glaze*1
mushroom_cream_bake|&fMushroom Cream Bake|MUSHROOM_STEW|UNCOMMON|FURNACE,SMOKER|90|0|c:mushroom_slice*3,c:cream*1,c:cheese*1
golden_vegetable_platter|&6Golden Vegetable Platter|GOLDEN_CARROT|EPIC|FURNACE,BLAST_FURNACE|160|0|c:carrot_slices*3,c:potato_slices*3,c:golden_sauce*1,c:honey_glaze*1
mana_berry_tea|&bMana Berry Tea|HONEY_BOTTLE|RARE|CAMPFIRE,FURNACE|0|100|c:berry_jam*1,c:mana_syrup*1,m:GLASS_BOTTLE*1
healing_herb_tea|&aHealing Herb Tea|HONEY_BOTTLE|UNCOMMON|CAMPFIRE,FURNACE|90|0|c:healing_herbs*2,c:honey_glaze*1,m:GLASS_BOTTLE*1
hot_chocolate|&6Hot Chocolate|HONEY_BOTTLE|UNCOMMON|CAMPFIRE,FURNACE|60|30|c:chocolate_mix*2,c:milk_bucket_custom*1,c:sugar_powder*1
spiced_mana_cider|&bSpiced Mana Cider|HONEY_BOTTLE|RARE|CAMPFIRE,FURNACE|0|120|c:apple_slices*3,c:mana_syrup*1,c:spice_mix*1,m:GLASS_BOTTLE*1
enchanted_cheese_bread|&aEnchanted Cheese Bread|BREAD|RARE|FURNACE,SMOKER|150|0|c:enchanted_dough*1,c:enchanted_cheese*1,c:butter*1
feast_of_the_campfire|&6Feast of the Campfire|COOKED_BEEF|LEGENDARY|CAMPFIRE,SOUL_CAMPFIRE|250|75|c:campfire_beef_bites*2,c:smoked_fish_skewer*2,c:vegetable_stew*1,c:honey_glazed_bread*1
""";
 public static List<Food> foods(){return DATA.lines().filter(s->!s.isBlank()).map(s->{String[]p=s.split("\\|");return new Food(p[0],p[1],p[2],p[3],List.of(p[4].split(",")),Integer.parseInt(p[5]),Integer.parseInt(p[6]),parse(p[7]));}).toList();}
 private static Map<String,Integer> parse(String raw){Map<String,Integer>m=new LinkedHashMap<>();for(String x:raw.split(",")){String[]p=x.split("\\*");m.put(p[0],Integer.parseInt(p[1]));}return Map.copyOf(m);}
 public static void apply(ConfigurationSection root){for(Food f:foods()){String b="custom-items.items."+f.id();if(!root.isConfigurationSection(b)){root.set(b+".name",f.name());root.set(b+".material",f.material());root.set(b+".type","CUSTOM_FOOD");root.set(b+".mmo.type","FOOD");root.set(b+".category","cooking");root.set(b+".rarity",f.rarity());root.set(b+".lore",List.of("&7Prepared at a cooking station."));root.set(b+".food.effect-type",f.heal()>0&&f.mana()>0?"MIXED":f.mana()>0?"MANA":"HEAL");root.set(b+".food.heal",f.heal());root.set(b+".food.mana",f.mana());root.set(b+".mmo.random-roll",false);root.set(b+".mmo.stats",null);root.set(b+".random-rarity",false);root.set(b+".ability.type","none");root.set(b+".max-stack-size",64);}String r="cooking.recipes."+f.id();root.set(r+".stations",f.stations());root.set(r+".ingredients",f.ingredients());root.set(r+".heal",f.heal());root.set(r+".mana",f.mana());}}
 public static Set<String> ids(){Set<String>s=new LinkedHashSet<>();foods().forEach(f->s.add(f.id()));return Set.copyOf(s);}
 public record Food(String id,String name,String material,String rarity,List<String> stations,int heal,int mana,Map<String,Integer> ingredients){}
}

