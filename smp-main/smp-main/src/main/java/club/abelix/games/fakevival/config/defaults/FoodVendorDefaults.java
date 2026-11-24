package club.abelix.games.fakevival.config.defaults;
import org.bukkit.configuration.file.YamlConfiguration;
import java.util.*;
public final class FoodVendorDefaults{
private FoodVendorDefaults(){}
private static final String DATA="""
food_vendor_campfire_cook|&6Campfire Cook|CAMPFIRE|campfire_toast:45;sweet_campfire_roll:60;campfire_fish_skewer:95;campfire_beef_bites:110;campfire_smores:140;grand_campfire_feast:6500;ultimate_campfire_platter:25000
food_vendor_baker|&eBaker|BREAD|crispy_flatbread:55;buttered_toast:70;cheesy_toast:95;garlic_flatbread:115;honey_glazed_bread:135;enchanted_bread_loaf:2500;royal_cheese_roll:4500
food_vendor_desserts|&dDessert Vendor|CAKE|chocolate_cookie:55;frosted_cookie:75;sweet_berry_tart:140;apple_hand_pie:150;pumpkin_spice_pie:220;chocolate_cake_slice:250;golden_honey_cake:2200;royal_bakers_feast:8000
food_vendor_fishermans_grill|&bFisherman's Grill|COOKED_COD|crispy_cod_bites:120;buttered_cod_plate:180;campfire_fish_skewer:95;smoked_fish_skewer:150;fish_sandwich:190;fishers_lunchbox:1250
food_vendor_seafood_chef|&3Seafood Chef|COOKED_SALMON|peppered_salmon_plate:190;honey_salmon_glaze:260;seaweed_salmon_bowl:300;salmon_seaweed_roll:260;deep_sea_sushi:340;royal_sushi_platter:2500;ocean_kings_feast:9500
food_vendor_butcher|&cButcher|COOKED_BEEF|beef_jerky:115;pork_cracklings:105;chicken_skewer:115;mutton_spice_roast:170;rabbit_herb_roast:180;pepper_steak_plate:450;butchers_feast:9000
food_vendor_smoked_meats|&6Smoked Meats Vendor|SMOKER|smoked_pork_slices:120;roasted_chicken_strips:110;spicy_meat_plate:420;golden_sauce_steak:1800;warrior_meat_stew:2400;smokers_feast:9000;combat_ration:1250
food_vendor_farm_cook|&aFarm Cook|GOLDEN_CARROT|roasted_carrot_bowl:90;loaded_potato_bowl:140;beetroot_cream_soup:135;vegetable_stew:210;farmer_stew:400;golden_farm_platter:2500;farmers_lunchbox:1200
food_vendor_soups|&aSoup Vendor|RABBIT_STEW|carrot_soup:85;pumpkin_soup:140;beetroot_stew_plus:130;mushroom_stew_custom:150;mushroom_bake:180;hearty_garden_stew:650;healers_garden_soup:2400
food_vendor_drinks|&fDrink Vendor|HONEY_BOTTLE|warm_milk:55;sweet_cream_drink:95;honey_milk:145;hot_chocolate:170;mana_berry_tea:500;healing_herb_tea:250;spiced_mana_cider:900
food_vendor_mana_food|&bMana Food Vendor|COOKIE|mana_cookie:450;mana_frosted_cookie:600;mana_cake_slice:850;mana_cream_drink:750;hot_mana_chocolate:900;frosted_mana_drink:1800;mana_royal_feast:15000
food_vendor_healing_food|&aHealing Food Vendor|GOLDEN_APPLE|healing_soup:700;healing_herb_tea:250;spiced_healing_tea:950;golden_healing_drink:2200;healers_garden_soup:2400;arcane_feast_drink:4000
food_vendor_spicy_food|&cSpicy Food Vendor|BLAZE_POWDER|spicy_salmon_skewer:350;puffer_spice_soup:390;spicy_fish_stew:550;spicy_cod_bowl:475;spiced_flatbread:130;pepper_steak_plate:450;warrior_meat_stew:2400
food_vendor_breakfast|&eBreakfast Vendor|EGG|fishermans_breakfast:500;garden_breakfast:380;hearty_breakfast_plate:900;campfire_toast:45;buttered_toast:70;hot_chocolate:170
food_vendor_lunchbox|&6Lunchbox Vendor|CHEST|miners_lunch:1200;foragers_snack:1050;fishers_lunchbox:1250;farmers_lunchbox:1200;combat_ration:1300;archers_ration:1250;mages_ration:1350;tank_ration:1500
food_vendor_feasts|&6Feast Vendor|CAKE|furnace_feast:9000;smokers_feast:9000;blast_furnace_feast:9500;grand_campfire_feast:6500;garden_royal_feast:9500;ocean_kings_feast:9500;royal_bakers_feast:8000;ultimate_campfire_platter:25000
food_vendor_nether_grill|&cNether Grill|MAGMA_BLOCK|puffer_heat_stew:400;spicy_meat_plate:420;spicy_fish_stew:550;hot_mana_chocolate:900;golden_sauce_steak:1800;tyrants_banquet:20000
food_vendor_soulfire_cook|&bSoulfire Cook|SOUL_CAMPFIRE|soulfire_tea:2200;soul_campfire_feast:8500;storm_fisher_stew:2000;sweet_campfire_roll:60;campfire_smores:140;mana_smore:750
food_vendor_royal_food|&6Royal Food Vendor|GOLDEN_CARROT|royal_cheese_roll:4500;royal_sushi_platter:2500;royal_bakers_feast:8000;royal_seafood_stew:2500;garden_royal_feast:9500;mana_royal_feast:15000;ultimate_campfire_platter:25000
food_vendor_traveling_food_merchant|&dTraveling Food Merchant|ENDER_CHEST|campfire_smores:140;fishers_lunchbox:1250;mana_cookie:450;combat_ration:1300;healers_garden_soup:2400;royal_sushi_platter:2500;golden_honey_cake:2200;ultimate_campfire_platter:25000
""";
public static void apply(YamlConfiguration c){for(String line:DATA.lines().filter(s->!s.isBlank()).toList()){String[]p=line.split("\\|",4);String b="shops."+p[0];c.set(b+".name",p[1]);c.set(b+".icon",p[2]);c.set(b+".description",List.of("&7Custom food and prepared meals.","","&eClick to browse!"));c.set(b+".buy-enabled",true);c.set(b+".sell-enabled",false);c.set(b+".sell-denied-message","&cThis vendor only sells food.");c.set(b+".items",null);for(String e:p[3].split(";")){String[]i=e.split(":");String q=b+".items."+i[0];int price=Integer.parseInt(i[1]);c.set(q+".custom-item",i[0]);c.set(q+".amount",1);c.set(q+".price",price);c.set(q+".sell-price",0);c.set(q+".allow-sell",false);c.set(q+".bulk-buy-enabled",price<5000);}String n="npcs.shops."+p[0];c.set(n+".shop",p[0]);c.set(n+".open-delay-ticks",10);c.set(n+".sound","ENTITY_VILLAGER_TRADE");}}
public static Set<String>shopIds(){Set<String>s=new LinkedHashSet<>();DATA.lines().filter(x->!x.isBlank()).forEach(x->s.add(x.substring(0,x.indexOf('|'))));return Set.copyOf(s);}
}

