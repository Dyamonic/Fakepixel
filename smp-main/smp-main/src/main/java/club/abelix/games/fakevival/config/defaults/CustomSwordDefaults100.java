package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class CustomSwordDefaults100 {
    private static final String DEFINITIONS = """
chipped_training_sword|Chipped Training Sword|WOODEN_SWORD|COMMON|18|6|1|2|Its chipped edge has taught countless first lessons.~The worn grip still sits firmly in a new warrior's hand.
rusty_guard_sword|Rusty Guard Sword|STONE_SWORD|COMMON|19|7|1|3|Rain has freckled the blade with rust.~A faded guard crest remains beneath the grime.
oakwood_broadsword|Oakwood Broadsword|WOODEN_SWORD|COMMON|20|8|1|4|Broad oak planks give this practice blade surprising weight.~Resin along the edge keeps the wood from splintering.
stonebound_sword|Stonebound Sword|STONE_SWORD|COMMON|21|9|2|3|Twine binds a sharpened stone edge to its hilt.~Each swing carries the weight of the hillside.
village_watch_sword|Village Watch Sword|IRON_SWORD|COMMON|22|8|1|5|Village sentries pass this blade down at every changing watch.~Its bell-shaped pommel is polished by nervous hands.
cracked_copper_sword|Cracked Copper Sword|STONE_SWORD|COMMON|20|10|2|4|A copper-colored seam runs through its cracked edge.~The metal hums faintly when rain approaches.
old_hunter_blade|Old Hunter Blade|IRON_SWORD|COMMON|23|9|2|5|Notches along the spine mark successful hunts.~Pine pitch and campfire smoke cling to the leather grip.
farmhand_saber|Farmhand Saber|WOODEN_SWORD|COMMON|19|11|1|6|A curved field blade adapted for roadside defense.~Its handle smells faintly of wheat and sun-warmed soil.
mossy_cave_sword|Mossy Cave Sword|STONE_SWORD|COMMON|24|8|2|4|Soft moss curls around the guard without dulling the edge.~Cold cave water beads across its stone surface.
riverstone_sword|Riverstone Sword|STONE_SWORD|COMMON|21|10|2|6|River stones were ground smooth to form this gray blade.~Its balanced shape follows the patient pull of a current.
dusty_bandit_blade|Dusty Bandit Blade|GOLDEN_SWORD|COMMON|25|7|3|5|Road dust lies deep in the wraps of this stolen weapon.~A crooked tally is scratched beneath the guard.
beginner_knight_sword|Beginner Knight Sword|IRON_SWORD|COMMON|22|12|1|7|Blunt tournament marks cover this young knight's blade.~The simple crossguard rewards a disciplined stance.
dull_iron_sword|Dull Iron Sword|IRON_SWORD|COMMON|26|9|2|6|Years of hurried sharpening have rounded its iron edge.~The blade remains dependable despite its tired shine.
worn_adventurer_sword|Worn Adventurer Sword|IRON_SWORD|COMMON|24|11|2|8|Scratches from distant roads cover both sides of the blade.~A frayed map cord is tied around its pommel.
hayvillage_shortblade|Hayvillage Shortblade|WOODEN_SWORD|COMMON|18|12|1|4|Crafted beside golden fields, this shortblade is quick to draw.~Braided straw beneath the grip cushions every strike.
forest_starter_sword|Forest Starter Sword|WOODEN_SWORD|COMMON|20|7|3|6|Fresh-cut wood gives the blade a clean woodland scent.~Leaf-shaped carvings guide a novice's grip.
fisher_guard_sword|Fisher Guard Sword|STONE_SWORD|COMMON|23|10|2|7|Salt and river spray have bleached the handle pale.~Its short edge is made for defending a crowded deck.
miner_defense_blade|Miner Defense Blade|IRON_SWORD|COMMON|27|12|1|8|A thick spine protects miners in cramped tunnels.~Coal dust darkens every groove around the guard.
wooden_hero_sword|Wooden Hero Sword|WOODEN_SWORD|COMMON|19|9|3|8|A child once carved a hero's crest into this wooden blade.~Careful repairs have made that hopeful mark endure.
cobbled_edge_sword|Cobbled Edge Sword|STONE_SWORD|COMMON|30|11|2|7|Cobbled fragments form a rough but stubborn cutting edge.~Mortar lines give the blade its uneven gray pattern.
reinforced_guard_sword|Reinforced Guard Sword|IRON_SWORD|UNCOMMON|38|14|3|9|Steel bands reinforce the blade from guard to point.~It was built to hold a gate when help was far away.
bronze_knight_blade|Bronze Knight Blade|GOLDEN_SWORD|UNCOMMON|40|16|3|10|Warm bronze tones shine beneath its polished surface.~A square pommel bears the mark of an early knightly order.
ironleaf_sword|Ironleaf Sword|IRON_SWORD|UNCOMMON|42|15|4|11|Layered metal leaves overlap along the spine.~The blade bends with a branch and returns straight.
desert_nomad_saber|Desert Nomad Saber|GOLDEN_SWORD|UNCOMMON|44|17|3|12|A sweeping curve keeps sand from clinging to the edge.~Blue cloth on the hilt recalls a distant desert well.
baywatch_cutlass|Baywatch Cutlass|IRON_SWORD|UNCOMMON|46|18|4|13|Harbor guards favor this cutlass on stormy night patrols.~Its brass basket hilt is green with ocean air.
riverfern_blade|Riverfern Blade|IRON_SWORD|UNCOMMON|41|19|3|14|Fern patterns ripple across the blade like shallow water.~A cool green wrap keeps the grip steady when wet.
frosted_iron_sword|Frosted Iron Sword|IRON_SWORD|UNCOMMON|48|20|4|15|A pale frost traces the fuller even beside a warm fire.~Its iron rings softly with the sound of cracking ice.
swampbone_sword|Swampbone Sword|STONE_SWORD|UNCOMMON|43|21|3|16|Bleached bone plates brace a dark stone edge.~Bog herbs packed into the handle mask its ancient odor.
cherry_guard_katana_blade|Cherry Guard Katana Blade|IRON_SWORD|UNCOMMON|50|17|4|17|Pink lacquer protects a slender guard blade from rain.~Falling petal marks record each completed patrol.
grainfield_defender|Grainfield Defender|GOLDEN_SWORD|UNCOMMON|45|22|3|18|Its golden edge flashes above tall fields like summer light.~Farmers ring the broad pommel to warn of raiders.
darkwood_sword|Darkwood Sword|WOODEN_SWORD|UNCOMMON|39|23|4|19|Dense darkwood makes this blade heavier than iron.~Silver root inlays catch what little light reaches the forest floor.
ember_touched_blade|Ember Touched Blade|GOLDEN_SWORD|UNCOMMON|52|18|3|20|Tiny embers drift from the edge after every forceful swing.~The heat never reaches the hand behind its blackened guard.
hunter_fang_sword|Hunter Fang Sword|IRON_SWORD|UNCOMMON|49|24|4|12|A hooked point follows the shape of a great beast's fang.~Claw marks on the scabbard serve as the hunter's signature.
steel_practice_sword|Steel Practice Sword|IRON_SWORD|UNCOMMON|47|20|3|14|Weighted steel turns daily drills into demanding work.~Rounded quillons protect the hands during long sparring sessions.
knight_apprentice_sword|Knight Apprentice Sword|IRON_SWORD|UNCOMMON|51|22|4|15|An apprentice's blue cord is wound neatly around the grip.~The blade is polished before every lesson without fail.
crystal_tipped_sword|Crystal Tipped Sword|DIAMOND_SWORD|UNCOMMON|54|19|3|16|A clear crystal cap strengthens the narrow point.~Colored light scatters across the floor when the sword is raised.
jungle_vine_sword|Jungle Vine Sword|WOODEN_SWORD|UNCOMMON|46|23|4|18|Living vines tighten around its carved wooden core.~The leaves turn toward nearby sunlight between battles.
wolftrack_saber|Wolftrack Saber|IRON_SWORD|UNCOMMON|53|21|3|17|Paired paw prints run from the hilt toward the point.~Its low whistle resembles a wolf moving through snow.
mushroom_spore_blade|Mushroom Spore Blade|GOLDEN_SWORD|UNCOMMON|50|24|4|19|Luminous spores drift from grooves along the blade.~A mycelium-bound grip stays warm in underground gardens.
copperforge_sword|Copperforge Sword|GOLDEN_SWORD|UNCOMMON|58|22|4|20|Copper forge scales give the blade a bright hammered finish.~Its heavy guard holds the warmth of a banked furnace.
royal_guard_sword|Royal Guard Sword|DIAMOND_SWORD|RARE|70|30|4|22|Blue enamel and gold trim identify the king's inner guard.~Its immaculate edge reflects the throne room windows.
imperial_soldier_blade|Imperial Soldier Blade|IRON_SWORD|RARE|72|32|5|24|Imperial ranks are stamped in precise lines below the guard.~The straight blade was forged for formation fighting.
purple_arcane_sword|Purple Arcane Sword|DIAMOND_SWORD|RARE|74|34|6|26|Violet runes brighten as the blade gathers momentum.~Quiet sparks orbit its amethyst-set pommel.
frostbite_saber|Frostbite Saber|DIAMOND_SWORD|RARE|76|36|5|28|Rime spreads over armor struck by this winter saber.~A shard of unmelted lake ice rests inside the hilt.
blazing_desert_sword|Blazing Desert Sword|GOLDEN_SWORD|RARE|78|38|6|30|Desert sunlight seems trapped beneath its golden edge.~Heat haze follows every broad and brilliant sweep.
drowned_bay_cutlass|Drowned Bay Cutlass|IRON_SWORD|RARE|80|40|5|32|Deep water stains cover this cutlass from a vanished fleet.~Barnacle scars along the guard point toward the open bay.
river_spirit_blade|River Spirit Blade|DIAMOND_SWORD|RARE|82|42|6|34|Silver currents move inside the translucent blade.~A river spirit's knot is tied beneath the pommel stone.
forest_ranger_sword|Forest Ranger Sword|IRON_SWORD|RARE|84|44|5|36|Rangers carry this green-edged sword beyond marked trails.~Its quiet sheath never snags on bramble or branch.
swamp_cursed_blade|Swamp Cursed Blade|STONE_SWORD|RARE|86|46|6|38|Dark water drips from the edge but leaves no puddle.~Whispered warnings rise from the guard after sunset.
cherry_blossom_sword|Cherry Blossom Sword|DIAMOND_SWORD|RARE|88|31|5|40|Petal-shaped sparks trail behind its graceful cuts.~A spring breeze lingers around the pale wooden scabbard.
mansion_duelist_sword|Mansion Duelist Sword|IRON_SWORD|RARE|90|33|6|42|A narrow point and jeweled guard suit candlelit duels.~The former owner's initials hide beneath a velvet wrap.
palm_temple_saber|Palm Temple Saber|GOLDEN_SWORD|RARE|92|35|5|43|Temple bells answer the saber's smooth circular motion.~Palm-leaf scripture is pressed into its sunlit grip.
grain_reaper_sword|Grain Reaper Sword|GOLDEN_SWORD|RARE|94|37|6|41|A harvest curve draws strength from sweeping attacks.~Golden chaff spins briefly in the wake of its edge.
shadowfang_blade|Shadowfang Blade|DIAMOND_SWORD|RARE|73|39|5|39|The dark point disappears when held against a shadow.~A silver fang at the pommel remains cold at noon.
crystal_guardian_sword|Crystal Guardian Sword|DIAMOND_SWORD|RARE|75|41|6|37|Faceted plates reinforce the guard against crushing blows.~Its crystal core rings when danger enters the chamber.
molten_iron_sword|Molten Iron Sword|IRON_SWORD|RARE|77|43|5|35|Molten orange veins pulse through otherwise dark iron.~Hammer marks glow whenever the blade tastes open air.
venom_edge_sword|Venom Edge Sword|DIAMOND_SWORD|RARE|79|45|6|33|Green venom collects in channels too narrow to touch.~The serpent guard watches every measured thrust.
moonlit_steel_sword|Moonlit Steel Sword|IRON_SWORD|RARE|81|47|5|34|Moonlight leaves a silver sheen that daylight cannot erase.~A crescent pommel balances the long steel edge.
cavebreaker_blade|Cavebreaker Blade|IRON_SWORD|RARE|83|36|6|31|A reinforced point splits stone in desperate tunnel fights.~Echoing impacts have darkened the grip with mineral dust.
enchanted_knight_sword|Enchanted Knight Sword|DIAMOND_SWORD|RARE|85|38|5|36|Old protective script circles the knightly crossguard.~The blade brightens near oaths honestly kept.
royal_champion_sword|Royal Champion Sword|DIAMOND_SWORD|EPIC|125|65|7|55|Only a crowned champion may carry this sapphire-edged sword.~Victory laurels flare gold across the guard after a duel.
imperial_officer_blade|Imperial Officer Blade|DIAMOND_SWORD|EPIC|126|66|7|56|A crimson officer's tassel hangs from its severe pommel.~Commands seem sharper when spoken over the raised blade.
arcane_spellblade|Arcane Spellblade|DIAMOND_SWORD|EPIC|127|67|7|57|Interlocking runes channel magic evenly along both edges.~The blade turns written spells into violet reflections.
frozen_warden_sword|Frozen Warden Sword|NETHERITE_SWORD|EPIC|128|68|7|58|Prison-cold chains are etched into its frostbound surface.~The air stills whenever the warden's blade leaves its sheath.
sunfire_saber|Sunfire Saber|GOLDEN_SWORD|EPIC|129|69|7|59|A white-hot line burns at the center of the curved saber.~Dawn-colored sparks bloom wherever its tip points.
tidecaller_cutlass|Tidecaller Cutlass|DIAMOND_SWORD|EPIC|130|70|7|60|The cutlass draws a rolling tide around every guarded step.~Distant surf sounds from the shell set in its pommel.
ancient_river_blade|Ancient River Blade|DIAMOND_SWORD|EPIC|131|71|7|61|Centuries of currents have polished this blade without dulling it.~Ancient river names flow across its silver-blue fuller.
forest_elder_sword|Forest Elder Sword|NETHERITE_SWORD|EPIC|132|72|7|62|Elder roots form a living cage around the metal hilt.~Birdsong follows the blade through even the darkest grove.
bogreaper_blade|Bogreaper Blade|NETHERITE_SWORD|EPIC|133|73|7|63|Black reeds bend toward this blade as it crosses a bog.~A sickly lantern glow burns behind its hooked guard.
sakura_spirit_sword|Sakura Spirit Sword|DIAMOND_SWORD|EPIC|134|74|7|64|A gentle spirit moves within the sword's rose-colored steel.~Spectral petals gather whenever its wielder stands calm.
mansion_secret_blade|Mansion Secret Blade|NETHERITE_SWORD|EPIC|135|75|7|65|Hidden panels once concealed this blade from unwelcome heirs.~Its mirror finish reveals doors that ordinary eyes overlook.
temple_relic_sword|Temple Relic Sword|GOLDEN_SWORD|EPIC|136|76|7|66|Incense smoke curls around a relic carried from a sealed shrine.~Sacred geometry covers the broad sun-gold guard.
golden_grain_scythe_sword|Golden Grain Scythe Sword|GOLDEN_SWORD|EPIC|137|77|7|67|A crescent edge gathers the strength of a hundred harvests.~Ripe grain bows in neat rows as the weapon passes.
voidfang_sword|Voidfang Sword|NETHERITE_SWORD|EPIC|138|78|7|68|A hollow darkness follows the fang-shaped point.~Stars vanish for a heartbeat in the blade's black reflection.
stormforged_blade|Stormforged Blade|NETHERITE_SWORD|EPIC|139|65|7|69|Thunder tempered this blade upon a mountaintop anvil.~Blue charge races between the guard's silver prongs.
magma_core_sword|Magma Core Sword|NETHERITE_SWORD|EPIC|140|66|7|70|A captive magma core turns slowly inside the reinforced hilt.~Crimson cracks close behind each searing swing.
toxic_venom_saber|Toxic Venom Saber|DIAMOND_SWORD|EPIC|141|67|7|71|Alchemical venom shines beneath the saber's translucent edge.~A sealed serpent vial balances the hooked pommel.
lunar_guard_sword|Lunar Guard Sword|NETHERITE_SWORD|EPIC|142|68|7|72|Moon guards raise this pale blade during silent midnight watches.~Its crescent guard casts light without flame.
deep_cavern_blade|Deep Cavern Blade|NETHERITE_SWORD|EPIC|143|69|7|55|Pressure from the deepest caverns hardened its dark metal.~Tiny crystals pulse along the edge like buried stars.
enchanted_royal_sword|Enchanted Royal Sword|DIAMOND_SWORD|EPIC|145|70|7|58|Royal enchanters layered seven protective seals into the steel.~A crown-shaped glow rests above the jeweled pommel.
kings_oath_blade|King's Oath Blade|NETHERITE_SWORD|LEGENDARY|220|120|10|100|The blade binds a ruler to every promise made before the realm.~Broken oaths leave a permanent shadow across its gold guard.
imperial_warlord_sword|Imperial Warlord Sword|NETHERITE_SWORD|LEGENDARY|222|122|10|102|A warlord's victories are engraved as marching columns of steel.~Its red banner wrap never tears in battle.
purple_void_spellblade|Purple Void Spellblade|NETHERITE_SWORD|LEGENDARY|224|124|10|104|Purple voidlight pours through runes cut beyond mortal depth.~Unspoken spells bend toward the blade's silent edge.
glacial_titan_sword|Glacial Titan Sword|NETHERITE_SWORD|LEGENDARY|226|126|10|106|Titan-forged ice plates make the enormous sword feel weightless.~Each impact leaves a glacier-blue fracture in the air.
solar_pharaoh_saber|Solar Pharaoh Saber|GOLDEN_SWORD|LEGENDARY|228|128|10|108|A pharaoh's captured noon burns within this royal saber.~Hieroglyphs awaken in gold when the sun reaches its peak.
abyssal_tide_cutlass|Abyssal Tide Cutlass|NETHERITE_SWORD|LEGENDARY|230|130|10|110|Pressure from the lightless sea coils around the cutlass.~A deep current answers every command of its black pearl.
river_deity_blade|River Deity Blade|DIAMOND_SWORD|LEGENDARY|232|132|10|112|A river deity shaped this edge from the first mountain spring.~Every stream recognizes the blue sigil on its guard.
ancient_forest_sword|Ancient Forest Sword|NETHERITE_SWORD|LEGENDARY|234|134|10|114|The oldest forest sealed a ring of living heartwood around the steel.~Roots rise beneath its wielder's steady footsteps.
cursed_swamp_reaver|Cursed Swamp Reaver|NETHERITE_SWORD|LEGENDARY|236|136|11|116|A drowned curse drags green fire along the reaver's edge.~Forgotten names whisper from beneath its corroded guard.
sakura_celestial_sword|Sakura Celestial Sword|DIAMOND_SWORD|LEGENDARY|238|138|11|118|Celestial petals orbit the blade in an endless spring.~Its mirrored steel holds a pink constellation unseen elsewhere.
mansion_ghostblade|Mansion Ghostblade|NETHERITE_SWORD|LEGENDARY|240|140|11|120|A vanished duelist still guides this blade through moonlit halls.~Cold footprints appear behind whoever draws it.
relic_sun_sword|Relic Sun Sword|GOLDEN_SWORD|LEGENDARY|242|142|11|122|A relic sun turns within the circular guard without setting.~Ancient prayers shine across its flawless golden edge.
harvest_lord_blade|Harvest Lord Blade|NETHERITE_SWORD|LEGENDARY|244|144|12|124|The harvest lord's seal grants weight to every sweeping cut.~Fields ripen beneath the shadow of its bronze crescent.
dragonsteel_sword|Dragonsteel Sword|NETHERITE_SWORD|LEGENDARY|246|146|12|126|Dragon flame and scale dust were folded into this dark steel.~The edge growls softly when it meets another weapon.
thunderking_blade|Thunderking Blade|NETHERITE_SWORD|LEGENDARY|248|148|12|128|A thunder king crowned this blade with a living bolt.~Storm clouds gather along the silver lightning guard.
blade_of_the_false_king|Blade of the False King|NETHERITE_SWORD|MYTHIC|400|220|15|200|A stolen crown fused itself to this blade when the false king fell.~Its reflection always shows an empty throne behind the wielder.
imperial_eternity_sword|Imperial Eternity Sword|NETHERITE_SWORD|MYTHIC|420|240|17|230|Every age of the empire is layered within its endless edge.~Golden legions march across the steel when history is threatened.
void_sakura_reaver|Void Sakura Reaver|NETHERITE_SWORD|MYTHIC|440|260|19|260|Black petals open into stars along the reaver's impossible curve.~The void falls silent to hear its single perfect draw.
primordial_dragon_sword|Primordial Dragon Sword|NETHERITE_SWORD|MYTHIC|460|280|21|290|The first dragon's breath still rages beneath the ancient scales.~Continents tremble in the low roar of its awakening edge.
celestial_worldbreaker|Celestial Worldbreaker|NETHERITE_SWORD|MYTHIC|480|300|23|320|Celestial iron circles a core taken from a shattered world.~Its final arc leaves constellations divided across the sky.
""";

    private static final Set<String> RECIPE_IDS = Set.of(
            "chipped_training_sword_recipe",
            "reinforced_guard_sword_recipe",
            "royal_guard_sword_recipe",
            "royal_champion_sword_recipe",
            "kings_oath_blade_recipe",
            "celestial_worldbreaker_recipe"
    );

    private CustomSwordDefaults100() {
    }

    public static void apply(ConfigurationSection root) {
        for (Definition definition : definitions()) {
            String base = "custom-items.items." + definition.id();
            root.set(base, null);
            root.set(base + ".name", rarityColor(definition.rarity()) + definition.name());
            root.set(base + ".material", definition.material());
            root.set(base + ".type", "SWORD");
            root.set(base + ".mmo.type", "SWORD");
            root.set(base + ".category", "weapons");
            root.set(base + ".subcategory", "swords");
            root.set(base + ".item-category", "WEAPONS");
            root.set(base + ".item-subcategory", "SWORDS");
            root.set(base + ".rarity", definition.rarity());
            root.set(base + ".lore", List.of(definition.lore().split("~", -1)));
            root.set(base + ".mmo.stats.DAMAGE", definition.damage());
            root.set(base + ".mmo.stats.STRENGTH", definition.strength());
            root.set(base + ".mmo.stats.CRIT_CHANCE", definition.critChance());
            root.set(base + ".mmo.stats.CRIT_DAMAGE", definition.critDamage());
            root.set(base + ".mmo.requirements.player_level", definition.playerLevel());
            root.set(base + ".mmo.random-roll", false);
            root.set(base + ".random-rarity", false);
            root.set(base + ".random-stats-allowed", true);
            root.set(base + ".random-rarity-allowed", true);
            root.set(base + ".reforge-allowed", true);
            root.set(base + ".enchant-allowed", true);
            root.set(base + ".fixed-rarity", false);
            root.set(base + ".special-item", false);
            root.set(base + ".developer-notebook", false);
            root.set(base + ".unbreakable", true);
            root.set(base + ".max-stack-size", 1);
            root.set(base + ".ability.type", "none");
            root.set(base + ".ability.mana-cost", 0);
            root.set(base + ".consumable.enabled", false);
        }
        recipe(root, "chipped_training_sword", 1, "WOODEN_SWORD", "STICK", 2, 1);
        recipe(root, "reinforced_guard_sword", 250, "IRON_INGOT", "STICK", 6, 1);
        recipe(root, "royal_guard_sword", 1000, "DIAMOND", "GOLD_INGOT", 4, 4);
        recipe(root, "royal_champion_sword", 2500, "DIAMOND_BLOCK", "GOLD_BLOCK", 2, 1);
        recipe(root, "kings_oath_blade", 5000, "NETHERITE_INGOT", "GOLD_BLOCK", 2, 2);
        recipe(root, "celestial_worldbreaker", 8000, "NETHERITE_BLOCK", "NETHER_STAR", 1, 1);
    }

    public static List<Definition> definitions() {
        List<Definition> definitions = new java.util.ArrayList<>();
        java.util.Map<String, Integer> rarityIndexes = new java.util.HashMap<>();
        for (String line : DEFINITIONS.lines().filter(value -> !value.isBlank()).toList()) {
            String[] parts = line.split("\\|", 9);
            int rarityIndex = rarityIndexes.merge(parts[3], 1, Integer::sum) - 1;
            definitions.add(new Definition(parts[0], parts[1], parts[2], parts[3], Integer.parseInt(parts[4]),
                    Integer.parseInt(parts[5]), Integer.parseInt(parts[6]), Integer.parseInt(parts[7]),
                    playerLevel(parts[3], rarityIndex), parts[8]));
        }
        definitions = List.copyOf(definitions);
        if (definitions.size() != 100) throw new IllegalStateException("Custom sword batch must contain 100 definitions");
        if (definitions.stream().map(Definition::id).collect(java.util.stream.Collectors.toSet()).size() != 100)
            throw new IllegalStateException("Custom sword batch contains duplicate IDs");
        return definitions;
    }

    public static Set<String> itemIds() {
        Set<String> ids = new LinkedHashSet<>();
        definitions().forEach(definition -> ids.add(definition.id()));
        return Set.copyOf(ids);
    }

    public static Set<String> recipeIds() {
        return RECIPE_IDS;
    }

    private static void recipe(ConfigurationSection root, String itemId, int playerLevel, String firstMaterial,
                               String secondMaterial, int firstAmount, int secondAmount) {
        String base = "recipes." + itemId + "_recipe";
        root.set(base, null);
        root.set(base + ".display-name", "&f" + definitions().stream().filter(definition -> definition.id().equals(itemId))
                .findFirst().orElseThrow().name());
        root.set(base + ".category", "WEAPONS");
        root.set(base + ".result", itemId);
        root.set(base + ".shape", List.of(" A ", " A ", " B "));
        root.set(base + ".ingredients.A", "material:" + firstMaterial);
        root.set(base + ".ingredients.B", "material:" + secondMaterial);
        root.set(base + ".amounts.A", firstAmount);
        root.set(base + ".amounts.B", secondAmount);
        root.set(base + ".unlock.type", "PLAYER_LEVEL");
        root.set(base + ".unlock.level", playerLevel);
    }

    private static String rarityColor(String rarity) {
        return switch (rarity) {
            case "UNCOMMON" -> "&a";
            case "RARE" -> "&9";
            case "EPIC" -> "&5";
            case "LEGENDARY" -> "&6";
            case "MYTHIC" -> "&d";
            default -> "&f";
        };
    }

    private static int playerLevel(String rarity, int index) {
        return switch (rarity) {
            case "UNCOMMON" -> 250 + index * 39;
            case "RARE" -> 1000 + index * 78;
            case "EPIC" -> 2500 + index * 131;
            case "LEGENDARY" -> 5000 + index * 214;
            case "MYTHIC" -> 8000 + index * 500;
            default -> 1 + index * 13;
        };
    }

    public record Definition(String id, String name, String material, String rarity, int damage, int strength,
                             int critChance, int critDamage, int playerLevel, String lore) {
    }
}
