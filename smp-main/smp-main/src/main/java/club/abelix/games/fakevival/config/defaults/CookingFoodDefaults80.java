package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;
import java.util.*;

public final class CookingFoodDefaults80 {
 private CookingFoodDefaults80(){}
 private static final String DATA="""
crispy_flatbread|&fCrispy Flatbread|BREAD|COMMON|FURNACE,CAMPFIRE|40|0|c:bread_dough*1,c:salt*1
sweet_campfire_roll|&eSweet Campfire Roll|BREAD|COMMON|CAMPFIRE,SOUL_CAMPFIRE|45|0|c:sweet_dough*1,c:sugar_powder*1
butter_honey_roll|&6Butter Honey Roll|BREAD|UNCOMMON|FURNACE|80|0|c:sweet_dough*1,c:butter*1,c:honey_glaze*1
cheese_stuffed_bread|&eCheese Stuffed Bread|BREAD|UNCOMMON|FURNACE,SMOKER|85|0|c:bread_dough*1,c:cheese*2
spiced_flatbread|&cSpiced Flatbread|BREAD|UNCOMMON|FURNACE,SMOKER|75|0|c:bread_dough*1,c:spice_mix*1,c:hot_spice_mix*1
frosted_cake_slice|&fFrosted Cake Slice|CAKE|UNCOMMON|FURNACE|90|0|c:cake_batter*1,c:frosting*2
chocolate_cake_slice|&6Chocolate Cake Slice|CAKE|RARE|FURNACE|110|0|c:cake_batter*1,c:chocolate_mix*2,c:frosting*1
mana_cake_slice|&bMana Cake Slice|CAKE|RARE|FURNACE|0|125|c:cake_batter*1,c:mana_syrup*2,c:frosting*1
apple_cinnamon_pie|&aApple Cinnamon Pie|PUMPKIN_PIE|RARE|FURNACE|115|0|c:pie_crust*1,c:apple_slices*4,c:spice_mix*1
melon_sugar_tart|&aMelon Sugar Tart|PUMPKIN_PIE|UNCOMMON|FURNACE|85|0|c:pie_crust*1,c:melon_chunks*4,c:sugar_powder*1
berry_frosting_tart|&dBerry Frosting Tart|PUMPKIN_PIE|RARE|FURNACE|105|0|c:pie_crust*1,c:berry_jam*2,c:frosting*1
golden_honey_cake|&6Golden Honey Cake|CAKE|EPIC|FURNACE|180|0|c:cake_batter*1,c:honey_glaze*2,c:golden_sauce*1
enchanted_bread_loaf|&aEnchanted Bread Loaf|BREAD|RARE|FURNACE|145|0|c:enchanted_dough*1,c:enchanted_butter*1
royal_cheese_roll|&6Royal Cheese Roll|BREAD|EPIC|FURNACE,SMOKER|200|0|c:enchanted_dough*1,c:enchanted_cheese*1,c:golden_sauce*1
campfire_smores|&6Campfire S'mores|COOKIE|UNCOMMON|CAMPFIRE,SOUL_CAMPFIRE|70|0|c:chocolate_mix*1,c:sweet_dough*1,c:sugar_powder*1
mana_smore|&bMana S'more|COOKIE|RARE|CAMPFIRE,SOUL_CAMPFIRE|0|100|c:campfire_smores*1,c:mana_syrup*1
crispy_cod_bites|&bCrispy Cod Bites|COOKED_COD|COMMON|CAMPFIRE,SMOKER|70|0|c:fish_fillet*2,c:flour*1,c:salt*1
buttered_cod_plate|&eButtered Cod Plate|COOKED_COD|UNCOMMON|FURNACE,SMOKER|95|0|c:cooked_fish_fillet*2,c:butter*1,c:salt*1
peppered_salmon_plate|&cPeppered Salmon Plate|COOKED_SALMON|UNCOMMON|SMOKER|100|0|c:grilled_salmon_fillet*2,c:pepper*1
honey_salmon_glaze|&6Honey Salmon Glaze|COOKED_SALMON|RARE|FURNACE,SMOKER|130|0|c:grilled_salmon_fillet*2,c:honey_glaze*1
spicy_cod_bowl|&cSpicy Cod Bowl|RABBIT_STEW|RARE|CAMPFIRE,FURNACE|120|0|c:cooked_fish_fillet*2,c:hot_spice_mix*1,c:seaweed_wrap*1
seaweed_salmon_bowl|&2Seaweed Salmon Bowl|RABBIT_STEW|RARE|CAMPFIRE,FURNACE|135|0|c:grilled_salmon_fillet*2,c:seaweed_wrap*2,c:salt*1
puffer_heat_stew|&ePuffer Heat Stew|RABBIT_STEW|RARE|CAMPFIRE,FURNACE|105|0|c:pufferfish_spice*2,c:hot_spice_mix*1,c:cream*1
fishermans_breakfast|&bFisherman's Breakfast|COOKED_COD|RARE|CAMPFIRE,SMOKER|145|0|c:cooked_fish_fillet*2,c:egg_mix*1,c:bread_dough*1
ocean_feast_bowl|&6Ocean Feast Bowl|RABBIT_STEW|EPIC|CAMPFIRE,FURNACE|220|50|c:cooked_fish_fillet*2,c:grilled_salmon_fillet*2,c:seaweed_wrap*1,c:pufferfish_spice*1
deep_sea_sushi|&bDeep Sea Sushi|KELP|RARE|CAMPFIRE,SMOKER|125|0|c:seaweed_wrap*2,c:fish_fillet*2,c:salmon_fillet*1
royal_sushi_platter|&6Royal Sushi Platter|KELP|EPIC|CAMPFIRE,SMOKER|210|0|c:deep_sea_sushi*1,c:golden_sauce*1,c:hot_spice_mix*1
storm_fisher_stew|&9Storm Fisher Stew|RABBIT_STEW|EPIC|SOUL_CAMPFIRE,FURNACE|140|90|c:cooked_fish_fillet*2,c:mana_syrup*1,c:seaweed_wrap*1
beef_jerky|&fBeef Jerky|COOKED_BEEF|COMMON|SMOKER,CAMPFIRE|65|0|c:raw_beef_slice*2,c:salt*1
pork_cracklings|&fPork Cracklings|COOKED_PORKCHOP|COMMON|SMOKER,CAMPFIRE|60|0|c:raw_pork_slice*2,c:salt*1
chicken_skewer|&fChicken Skewer|COOKED_CHICKEN|COMMON|CAMPFIRE,SMOKER|65|0|c:raw_chicken_slice*2,m:STICK*1,c:pepper*1
mutton_spice_roast|&aMutton Spice Roast|COOKED_MUTTON|UNCOMMON|SMOKER,FURNACE|90|0|c:raw_mutton_slice*2,c:spice_mix*1
rabbit_herb_roast|&aRabbit Herb Roast|COOKED_RABBIT|UNCOMMON|SMOKER,FURNACE|95|0|c:raw_rabbit_slice*2,c:healing_herbs*1
pepper_steak_plate|&cPepper Steak Plate|COOKED_BEEF|RARE|SMOKER,BLAST_FURNACE|140|0|c:cooked_meat_slice*3,c:pepper*2,c:butter*1
honey_pork_roast|&6Honey Pork Roast|COOKED_PORKCHOP|RARE|SMOKER,FURNACE|135|0|c:raw_pork_slice*3,c:honey_glaze*1,c:spice_mix*1
golden_chicken_plate|&6Golden Chicken Plate|COOKED_CHICKEN|EPIC|SMOKER,BLAST_FURNACE|190|0|c:raw_chicken_slice*3,c:golden_sauce*1,c:hot_spice_mix*1
warrior_meat_stew|&cWarrior Meat Stew|RABBIT_STEW|EPIC|CAMPFIRE,FURNACE|210|0|c:cooked_meat_slice*3,c:hot_spice_mix*1,c:healing_herbs*1
butchers_feast|&6Butcher's Feast|COOKED_BEEF|LEGENDARY|SMOKER,BLAST_FURNACE|300|0|c:cooked_meat_slice*4,c:golden_sauce*2,c:hot_spice_mix*1
carrot_soup|&6Carrot Soup|RABBIT_STEW|COMMON|CAMPFIRE,FURNACE|55|0|c:carrot_slices*3,c:cream*1
potato_skillet|&ePotato Skillet|BAKED_POTATO|COMMON|CAMPFIRE,FURNACE|60|0|c:potato_slices*3,c:butter*1
beetroot_stew_plus|&cBeetroot Stew+|BEETROOT_SOUP|UNCOMMON|CAMPFIRE,FURNACE|75|0|c:beetroot_slices*3,c:spice_mix*1
pumpkin_soup|&6Pumpkin Soup|RABBIT_STEW|UNCOMMON|CAMPFIRE,FURNACE|85|0|c:pumpkin_puree*2,c:cream*1,c:salt*1
melon_smoothie|&aMelon Smoothie|HONEY_BOTTLE|UNCOMMON|FURNACE|70|0|c:melon_juice*2,c:sugar_powder*1,m:GLASS_BOTTLE*1
mushroom_bake|&fMushroom Bake|MUSHROOM_STEW|UNCOMMON|FURNACE,SMOKER|95|0|c:mushroom_slice*3,c:cheese*1
garden_breakfast|&aGarden Breakfast|RABBIT_STEW|RARE|CAMPFIRE,FURNACE|120|0|c:carrot_slices*2,c:potato_slices*2,c:egg_mix*1,c:butter*1
farmer_stew|&aFarmer Stew|RABBIT_STEW|RARE|CAMPFIRE,FURNACE|135|0|c:carrot_slices*2,c:potato_slices*2,c:beetroot_slices*2,c:pumpkin_puree*1
golden_farm_platter|&6Golden Farm Platter|GOLDEN_CARROT|EPIC|FURNACE,BLAST_FURNACE|200|0|c:carrot_slices*3,c:potato_slices*3,c:melon_chunks*3,c:golden_sauce*1
healers_garden_soup|&aHealer's Garden Soup|RABBIT_STEW|EPIC|CAMPFIRE,FURNACE|180|80|c:farmer_stew*1,c:healing_herbs*2,c:mana_syrup*1
warm_milk|&fWarm Milk|MILK_BUCKET|COMMON|CAMPFIRE,FURNACE|25|0|c:milk_bucket_custom*1
sweet_cream_drink|&fSweet Cream Drink|HONEY_BOTTLE|UNCOMMON|FURNACE|45|0|c:cream*1,c:sugar_powder*1,m:GLASS_BOTTLE*1
honey_milk|&6Honey Milk|HONEY_BOTTLE|UNCOMMON|CAMPFIRE,FURNACE|75|0|c:milk_bucket_custom*1,c:honey_glaze*1,m:GLASS_BOTTLE*1
mana_cream_drink|&bMana Cream Drink|HONEY_BOTTLE|RARE|FURNACE|0|110|c:cream*1,c:mana_syrup*1,m:GLASS_BOTTLE*1
hot_mana_chocolate|&bHot Mana Chocolate|HONEY_BOTTLE|RARE|CAMPFIRE,FURNACE|60|90|c:chocolate_mix*2,c:mana_syrup*1,c:milk_bucket_custom*1
spiced_healing_tea|&aSpiced Healing Tea|HONEY_BOTTLE|RARE|CAMPFIRE,SOUL_CAMPFIRE|130|0|c:healing_herbs*2,c:spice_mix*1,m:GLASS_BOTTLE*1
frosted_mana_drink|&bFrosted Mana Drink|HONEY_BOTTLE|EPIC|FURNACE|0|160|c:mana_syrup*2,c:frosting*1,m:GLASS_BOTTLE*1
golden_healing_drink|&6Golden Healing Drink|HONEY_BOTTLE|EPIC|FURNACE,BLAST_FURNACE|200|0|c:golden_sauce*1,c:healing_herbs*2,m:GLASS_BOTTLE*1
soulfire_tea|&bSoulfire Tea|HONEY_BOTTLE|EPIC|SOUL_CAMPFIRE|0|120|c:mana_syrup*1,c:hot_spice_mix*1,m:GLASS_BOTTLE*1
arcane_feast_drink|&dArcane Feast Drink|HONEY_BOTTLE|LEGENDARY|FURNACE,BLAST_FURNACE|150|250|c:mana_syrup*3,c:golden_sauce*1,c:healing_herbs*2
furnace_feast|&6Furnace Feast|COOKED_BEEF|LEGENDARY|FURNACE|300|100|c:pepper_steak_plate*1,c:fish_sandwich*1,c:farmer_stew*1
smokers_feast|&6Smoker's Feast|COOKED_BEEF|LEGENDARY|SMOKER|325|0|c:butchers_feast*1,c:smoked_fish_skewer*1,c:mushroom_bake*1
blast_furnace_feast|&6Blast Furnace Feast|COOKED_BEEF|LEGENDARY|BLAST_FURNACE|350|0|c:golden_chicken_plate*1,c:golden_farm_platter*1,c:golden_sauce*1
soul_campfire_feast|&bSoul Campfire Feast|RABBIT_STEW|LEGENDARY|SOUL_CAMPFIRE|300|150|c:soulfire_tea*1,c:spicy_cod_bowl*1,c:warrior_meat_stew*1
grand_campfire_feast|&6Grand Campfire Feast|COOKED_BEEF|LEGENDARY|CAMPFIRE,SOUL_CAMPFIRE|275|75|c:campfire_smores*2,c:campfire_fish_skewer*2,c:chicken_skewer*2,c:carrot_soup*1
royal_bakers_feast|&6Royal Baker's Feast|CAKE|LEGENDARY|FURNACE|280|120|c:royal_cheese_roll*1,c:chocolate_cake_slice*1,c:apple_cinnamon_pie*1
ocean_kings_feast|&6Ocean King's Feast|COOKED_SALMON|LEGENDARY|CAMPFIRE,SMOKER|330|120|c:ocean_feast_bowl*1,c:royal_sushi_platter*1,c:storm_fisher_stew*1
garden_royal_feast|&6Garden Royal Feast|GOLDEN_CARROT|LEGENDARY|FURNACE|320|0|c:healers_garden_soup*1,c:golden_farm_platter*1,c:honey_glazed_bread*1
mana_royal_feast|&dMana Royal Feast|CAKE|MYTHIC|FURNACE,BLAST_FURNACE|150|400|c:arcane_feast_drink*1,c:mana_cake_slice*1,c:mana_cookie*2
tyrants_banquet|&4Tyrant's Banquet|COOKED_BEEF|MYTHIC|BLAST_FURNACE|450|0|c:butchers_feast*1,c:golden_sauce_steak*1,c:hot_spice_mix*2
miners_lunch|&7Miner's Lunch|BAKED_POTATO|RARE|FURNACE|140|0|c:loaded_potato_bowl*1,c:cheese_bread*1
foragers_snack|&aForager's Snack|COOKIE|RARE|CAMPFIRE|120|0|c:sweet_berry_tart*1,c:honey_glaze*1
fishers_lunchbox|&bFisher's Lunchbox|COOKED_COD|RARE|SMOKER|150|0|c:smoked_fish_skewer*1,c:seaweed_fish_roll*1
farmers_lunchbox|&aFarmer's Lunchbox|GOLDEN_CARROT|RARE|FURNACE|150|0|c:roasted_carrot_bowl*1,c:loaded_potato_bowl*1,c:honey_bread*1
combat_ration|&cCombat Ration|COOKED_BEEF|RARE|SMOKER|160|0|c:cooked_meat_slice*2,c:spicy_fish_stew*1
archers_ration|&aArcher's Ration|COOKED_CHICKEN|RARE|SMOKER|140|0|c:roasted_chicken_strips*2,c:apple_slices*2,c:honey_glaze*1
mages_ration|&bMage's Ration|COOKIE|RARE|FURNACE|0|170|c:mana_cookie*2,c:mana_syrup*1
tank_ration|&7Tank Ration|COOKED_BEEF|RARE|BLAST_FURNACE|180|0|c:golden_sauce_steak*1,c:cheese_bread*1
speedy_sugar_cookie|&eSpeedy Sugar Cookie|COOKIE|UNCOMMON|FURNACE|35|0|c:cookie_dough*1,c:sugar_powder*2
lucky_carrot_cake|&6Lucky Carrot Cake|CAKE|EPIC|FURNACE|180|0|c:cake_batter*1,c:carrot_slices*4,c:golden_sauce*1
hearty_breakfast_plate|&eHearty Breakfast Plate|COOKED_BEEF|RARE|FURNACE,SMOKER|145|0|c:egg_mix*1,c:campfire_toast*1,c:cooked_meat_slice*1
ultimate_campfire_platter|&6Ultimate Campfire Platter|COOKED_BEEF|MYTHIC|CAMPFIRE,SOUL_CAMPFIRE|500|200|c:grand_campfire_feast*1,c:ocean_kings_feast*1,c:garden_royal_feast*1
""";
 public static List<CookingFoodDefaults40.Food> foods(){return DATA.lines().filter(s->!s.isBlank()).map(s->{String[]p=s.split("\\|");return new CookingFoodDefaults40.Food(p[0],p[1],p[2],p[3],List.of(p[4].split(",")),Integer.parseInt(p[5]),Integer.parseInt(p[6]),parse(p[7]));}).toList();}
 private static Map<String,Integer>parse(String raw){Map<String,Integer>m=new LinkedHashMap<>();for(String x:raw.split(",")){String[]p=x.split("\\*");m.put(p[0],Integer.parseInt(p[1]));}return Map.copyOf(m);}
 public static void apply(ConfigurationSection root){for(var f:foods()){String b="custom-items.items."+f.id();if(!root.isConfigurationSection(b)){root.set(b+".name",f.name());root.set(b+".material",f.material());root.set(b+".type","CUSTOM_FOOD");root.set(b+".mmo.type","FOOD");root.set(b+".category","cooking");root.set(b+".rarity",f.rarity());root.set(b+".lore",List.of("&7Prepared at a cooking station."));root.set(b+".food.effect-type",f.heal()>0&&f.mana()>0?"MIXED":f.mana()>0?"MANA":"HEAL");root.set(b+".food.heal",f.heal());root.set(b+".food.mana",f.mana());root.set(b+".mmo.random-roll",false);root.set(b+".mmo.stats",null);root.set(b+".random-rarity",false);root.set(b+".ability.type","none");root.set(b+".ability.mana-cost",0);root.set(b+".max-stack-size",64);}String r="cooking.recipes."+f.id();root.set(r+".stations",f.stations());root.set(r+".ingredients",f.ingredients());root.set(r+".heal",f.heal());root.set(r+".mana",f.mana());}}
 public static Set<String>ids(){Set<String>s=new LinkedHashSet<>();foods().forEach(f->s.add(f.id()));return Set.copyOf(s);}
}

