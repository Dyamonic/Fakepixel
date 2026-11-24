package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Java-only registry for eighty catchable, stat-free custom fish. */
public final class CustomFishDefaults80 {
    private CustomFishDefaults80() {}
    private static final String DATA="""
brook_minnow|&fBrook Minnow|COD|COMMON|9|5|18|
pebble_perch|&fPebble Perch|COD|COMMON|13|12|38|
green_gill|&fGreen Gill|COD|COMMON|12|10|32|
shallow_darter|&fShallow Darter|COD|COMMON|11|8|26|
river_roach|&fRiver Roach|COD|COMMON|15|18|55|
mud_whisker|&fMud Whisker|COD|COMMON|18|25|95|
pale_herring|&fPale Herring|COD|COMMON|13|14|40|
driftwood_carp|&fDriftwood Carp|COD|COMMON|20|35|120|
sunny_sardine|&fSunny Sardine|COD|COMMON|10|6|22|
tiny_tilapia|&fTiny Tilapia|TROPICAL_FISH|COMMON|14|8|28|
pond_bass|&fPond Bass|COD|COMMON|16|20|70|
reed_fish|&fReed Fish|COD|COMMON|13|12|45|
salt_cod|&fSalt Cod|COD|COMMON|17|25|80|
lazy_salmon|&fLazy Salmon|SALMON|COMMON|19|35|100|
bay_flounder|&fBay Flounder|COD|COMMON|18|25|75|
amber_trout|&aAmber Trout|SALMON|UNCOMMON|45|30|105|
jade_carp|&aJade Carp|COD|UNCOMMON|42|25|90|
spotted_pike|&aSpotted Pike|COD|UNCOMMON|50|40|140|
copperfin_tuna|&aCopperfin Tuna|SALMON|UNCOMMON|58|55|150|
lime_clownfish|&aLime Clownfish|TROPICAL_FISH|UNCOMMON|39|8|24|
bubblefin_cod|&aBubblefin Cod|COD|UNCOMMON|44|30|95|
orange_reef_fish|&aOrange Reef Fish|TROPICAL_FISH|UNCOMMON|41|10|30|
striped_eel|&aStriped Eel|COD|UNCOMMON|52|45|155|
needle_puffer|&aNeedle Puffer|PUFFERFISH|UNCOMMON|55|15|45|
moonlit_bass|&aMoonlit Bass|COD|UNCOMMON|60|35|115|
river_goldenfish|&aRiver Goldenfish|TROPICAL_FISH|UNCOMMON|62|10|34|
ironbelly_catfish|&aIronbelly Catfish|COD|UNCOMMON|70|60|180|
silverback_salmon|&aSilverback Salmon|SALMON|UNCOMMON|64|50|145|
coral_darter|&aCoral Darter|TROPICAL_FISH|UNCOMMON|48|8|26|
mossback_carp|&aMossback Carp|COD|UNCOMMON|66|35|130|
diamondscale_koi|&9Diamondscale Koi|TROPICAL_FISH|RARE|175|25|95|
ancient_river_gar|&9Ancient River Gar|COD|RARE|190|90|260|
frostfin_tuna|&9Frostfin Tuna|SALMON|RARE|210|90|270|
shadow_carp|&9Shadow Carp|COD|RARE|180|50|160|
crystal_snapper|&9Crystal Snapper|SALMON|RARE|195|45|150|
glowtail_eel|&9Glowtail Eel|COD|RARE|205|80|250|
royal_puffer|&9Royal Puffer|PUFFERFISH|RARE|188|20|65|
sapphire_bass|&9Sapphire Bass|COD|RARE|172|40|135|
opal_angelfish|&9Opal Angelfish|TROPICAL_FISH|RARE|185|12|40|
stormscale_salmon|&9Stormscale Salmon|SALMON|RARE|220|70|200|
obsidian_catfish|&9Obsidian Catfish|COD|RARE|230|80|260|
phantom_herring|&9Phantom Herring|COD|RARE|165|25|90|
sunset_trout|&9Sunset Trout|SALMON|RARE|170|30|120|
pearlfin_guppy|&9Pearlfin Guppy|TROPICAL_FISH|RARE|160|8|25|
elder_pike|&9Elder Pike|COD|RARE|240|90|280|
voidfin_tuna|&5Voidfin Tuna|SALMON|EPIC|520|120|300|
abyss_wraith_eel|&5Abyss Wraith Eel|COD|EPIC|560|110|300|
infernal_snapper|&5Infernal Snapper|SALMON|EPIC|540|60|180|
runestone_koi|&5Runestone Koi|TROPICAL_FISH|EPIC|500|35|125|
twilight_ray|&5Twilight Ray|COD|EPIC|575|90|260|
emberback_whale|&5Emberback Whale|SALMON|EPIC|650|170|300|
deepstone_sturgeon|&5Deepstone Sturgeon|SALMON|EPIC|620|150|300|
spirit_lanternfish|&5Spirit Lanternfish|COD|EPIC|590|30|100|
arcane_puffer|&5Arcane Puffer|PUFFERFISH|EPIC|510|20|70|
dreamscale_fish|&5Dreamscale Fish|TROPICAL_FISH|EPIC|600|20|80|
sunlord_leviathan|&6Sunlord Leviathan|SALMON|LEGENDARY|2200|180|300|
moonlord_leviathan|&6Moonlord Leviathan|SALMON|LEGENDARY|2200|180|300|
crimson_megalodon|&6Crimson Megalodon|COD|LEGENDARY|2400|190|300|
azure_megalodon|&6Azure Megalodon|COD|LEGENDARY|2400|190|300|
golden_oracle_koi|&6Golden Oracle Koi|TROPICAL_FISH|LEGENDARY|2000|45|135|
ancient_whale|&6Ancient Whale|SALMON|LEGENDARY|2600|200|300|
storm_kraken_fry|&6Storm Kraken Fry|COD|LEGENDARY|2300|100|220|
royal_sea_serpent|&6Royal Sea Serpent|COD|LEGENDARY|2500|160|300|
celestial_sturgeon|&6Celestial Sturgeon|SALMON|LEGENDARY|2700|160|300|
dragonfin_tuna|&6Dragonfin Tuna|SALMON|LEGENDARY|2750|150|300|
void_emperor_whale|&dVoid Emperor Whale|SALMON|MYTHIC|7500|200|300|
cosmic_kraken_spawn|&dCosmic Kraken Spawn|COD|MYTHIC|7000|120|260|
starborn_angler|&dStarborn Angler|TROPICAL_FISH|MYTHIC|6800|40|150|
abyssal_crownfish|&dAbyssal Crownfish|TROPICAL_FISH|MYTHIC|7200|45|160|
eternal_void_ray|&dEternal Void Ray|COD|MYTHIC|8000|160|300|
midnight_cod|&9Midnight Cod|COD|RARE|230|25|100|NIGHT
sunrise_salmon|&9Sunrise Salmon|SALMON|RARE|230|50|155|MORNING
rainbow_reef_fish|&5Rainbow Reef Fish|TROPICAL_FISH|EPIC|650|12|45|WARM_OCEAN
cold_ocean_cod|&aCold Ocean Cod|COD|UNCOMMON|75|30|120|COLD_OCEAN
frozen_lake_perch|&9Frozen Lake Perch|COD|RARE|250|20|80|FROZEN
lush_cave_axofish|&5Lush Cave Axofish|TROPICAL_FISH|EPIC|700|15|55|LUSH_CAVES
dripstone_cave_eel|&9Dripstone Cave Eel|COD|RARE|275|60|200|DRIPSTONE_CAVES
mangrove_mudskipper|&aMangrove Mudskipper|COD|UNCOMMON|80|12|35|MANGROVE_SWAMP
flower_lake_guppy|&aFlower Lake Guppy|TROPICAL_FISH|UNCOMMON|78|6|22|FLOWER_FOREST
bloodmoon_piranha|&6Bloodmoon Piranha|COD|LEGENDARY|3000|30|95|NIGHT_RARE
""";

    public static void apply(ConfigurationSection root){for(Fish fish:fish()){String b="custom-items.items."+fish.id();if(root.isConfigurationSection(b))continue;root.set(b+".name",fish.name());root.set(b+".material",fish.material());root.set(b+".type","CUSTOM_FISH");root.set(b+".mmo.type","MATERIAL");root.set(b+".category","custom_fish");root.set(b+".rarity",fish.rarity());root.set(b+".lore",List.of("&7A catchable custom fish."));root.set(b+".fish.base-price",fish.basePrice());root.set(b+".fish.min-length-cm",fish.minCm());root.set(b+".fish.max-length-cm",fish.maxCm());root.set(b+".fish.condition",fish.condition());root.set(b+".mmo.random-roll",false);root.set(b+".mmo.stats",null);root.set(b+".random-rarity",false);root.set(b+".ability.type","none");root.set(b+".ability.mana-cost",0);root.set(b+".max-stack-size",1);root.set(b+".guild-sellable",true);root.set(b+".sellall-allowed",true);root.set(b+".tradeable",true);root.set(b+".soulbound",false);}collection(root,"custom_fish","&bCustom Fish Collection",ids().stream().toList());collection(root,"rare_fish","&9Rare Fish Collection",List.of("rare_fish_marker"));collection(root,"giant_fish","&6Giant Fish Collection",List.of("giant_fish_marker"));collection(root,"biome_fish","&5Biome Fish Collection",List.of("biome_fish_marker"));}
    private static void collection(ConfigurationSection root,String id,String name,List<String> materials){String b="collections.items."+id;root.set(b+".name",name);root.set(b+".category","fishing");root.set(b+".materials",materials);root.set(b+".tiers",List.of(50,100,250,500,1000,2500,5000,10000,25000,50000));}
    public static List<Fish> fish(){return DATA.lines().filter(s->!s.isBlank()).map(s->{String[]p=s.split("\\|",-1);return new Fish(p[0],p[1],p[2],p[3],Integer.parseInt(p[4]),Integer.parseInt(p[5]),Integer.parseInt(p[6]),p[7]);}).toList();}
    public static Set<String> ids(){Set<String>s=new LinkedHashSet<>();fish().forEach(f->s.add(f.id()));return Set.copyOf(s);}
    public record Fish(String id,String name,String material,String rarity,int basePrice,int minCm,int maxCm,String condition){}
}
