package club.abelix.games.fakevival.config.defaults;

import club.abelix.games.fakevival.mmo.MMOStatType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CustomEnchantDefaults100 {
    private static final String DEFINITIONS = """
battle_edge|Battle Edge|5|weapon|SWORD,KATANA,DAGGER|UNCOMMON|Increases Damage by 4 per level.|stat|PASSIVE|DAMAGE=4||
sharpened_soul|Sharpened Soul|5|weapon|SWORD,KATANA,DAGGER|UNCOMMON|Increases Strength by 3 per level.|stat|PASSIVE|STRENGTH=3||
executioner|Executioner|5|weapon|SWORD,KATANA,DAGGER|RARE|Deals 3% more damage per level to mobs below 35% health.|executioner|ON_HIT|||
first_strike_custom|First Strike|5|weapon|SWORD,KATANA,DAGGER|RARE|Deals 8% more damage per level on the first hit against a target.|first_strike_custom|ON_HIT|||
final_wound|Final Wound|5|weapon|SWORD,KATANA,DAGGER|EPIC|Deals 5% more damage per level to targets below 20% health.|final_wound|ON_HIT|||
royal_cleave|Royal Cleave|5|weapon|SWORD,KATANA,DAGGER|EPIC|Damages nearby hostile mobs for part of the hit damage.|royal_cleave|ON_HIT|||
blood_drinker|Blood Drinker|5|weapon|SWORD,KATANA,DAGGER|EPIC|Heals 1% of damage dealt per level.|blood_drinker|ON_HIT|||
venom_tip|Venom Tip|4|weapon|SWORD,KATANA,DAGGER|RARE|Poisons hostile mobs for 1 second per level.|venom_tip|ON_HIT|||
frost_cut|Frost Cut|4|weapon|SWORD,KATANA,DAGGER|RARE|Slows hostile mobs for 1 second per level.|frost_cut|ON_HIT|||
thunder_edge|Thunder Edge|5|weapon|SWORD,KATANA,DAGGER|EPIC|May strike visual lightning and deal bonus magic damage.|thunder_edge|ON_HIT|||
ragebound|Ragebound|5|weapon|SWORD,KATANA,DAGGER|RARE|Grants 2 Strength per level while below half health.|ragebound|PASSIVE|||
lifebreaker|Lifebreaker|5|weapon|SWORD,KATANA,DAGGER|RARE|Deals 2% more damage per level against high-health mobs.|lifebreaker|ON_HIT|||
armor_piercer|Armor Piercer|5|weapon|SWORD,KATANA,DAGGER|EPIC|Ignores 2% of mob defense per level.|armor_piercer|ON_HIT|||
swift_blade|Swift Blade|5|weapon|SWORD,KATANA,DAGGER|UNCOMMON|Increases Speed by 1 per level while held.|stat|PASSIVE|SPEED=1||
combo_edge|Combo Edge|5|weapon|SWORD,KATANA,DAGGER|RARE|Consecutive hits gain damage and reset after 4 seconds.|combo_edge|ON_HIT|||
dragon_rend|Dragon Rend|5|weapon|SWORD,KATANA,DAGGER|LEGENDARY|Deals 4% more damage per level against bosses.|dragon_rend|ON_HIT|||
shadow_slash|Shadow Slash|5|weapon|SWORD,KATANA,DAGGER|EPIC|May deal bonus dark damage on hit.|shadow_slash|ON_HIT|||
holy_strike|Holy Strike|5|weapon|SWORD,KATANA,DAGGER|RARE|Deals 5% more damage per level against undead mobs.|holy_strike|ON_HIT|||
beast_slayer|Beast Slayer|5|weapon|SWORD,KATANA,DAGGER|RARE|Deals 5% more damage per level against beast mobs.|beast_slayer|ON_HIT|||
dungeon_bane|Dungeon Bane|5|weapon|SWORD,KATANA,DAGGER|EPIC|Deals 4% more damage per level inside dungeon runs.|dungeon_bane|ON_HIT|||
arrow_force|Arrow Force|5|bow|BOW,SHORTBOW,LONGBOW,CROSSBOW|UNCOMMON|Increases arrow Damage by 5 per level.|stat|PROJECTILE_HIT|DAMAGE=5||
snipe_custom|Snipe|5|bow|BOW,SHORTBOW,LONGBOW,CROSSBOW|RARE|Increases ranged damage for every 10 blocks traveled.|snipe_custom|PROJECTILE_HIT|||
piercing_shot|Piercing Shot|4|bow|BOW,SHORTBOW,LONGBOW,CROSSBOW|RARE|Arrows pierce an additional mob every 2 levels.|piercing_shot|PROJECTILE_LAUNCH|||
quick_draw|Quick Draw|5|bow|BOW,SHORTBOW,LONGBOW,CROSSBOW|UNCOMMON|Increases projectile speed by 3% per level.|quick_draw|PROJECTILE_LAUNCH|||
hunter_focus|Hunter Focus|5|bow|BOW,SHORTBOW,LONGBOW,CROSSBOW|RARE|Increases bow Crit Chance by 2 per level.|stat|PASSIVE|CRIT_CHANCE=2||
eagle_eye|Eagle Eye|5|bow|BOW,SHORTBOW,LONGBOW,CROSSBOW|RARE|Increases arrow Crit Damage by 6 per level.|stat|PASSIVE|CRIT_DAMAGE=6||
burning_arrow|Burning Arrow|4|bow|BOW,SHORTBOW,LONGBOW,CROSSBOW|RARE|Burns hostile mobs for 1 second per level.|burning_arrow|PROJECTILE_HIT|||
frost_arrow|Frost Arrow|4|bow|BOW,SHORTBOW,LONGBOW,CROSSBOW|RARE|Slows hostile mobs struck by arrows.|frost_arrow|PROJECTILE_HIT|||
storm_arrow|Storm Arrow|5|bow|BOW,SHORTBOW,LONGBOW,CROSSBOW|EPIC|May chain visual lightning damage to nearby hostile mobs.|storm_arrow|PROJECTILE_HIT|||
bounty_shot|Bounty Shot|5|bow|BOW,SHORTBOW,LONGBOW,CROSSBOW|EPIC|Increases coins earned from bow kills by 2% per level.|bounty_shot|KILL_ENTITY|||
deep_lure|Deep Lure|5|fishing|FISHING_ROD|UNCOMMON|Increases Fishing Speed by 3 per level.|stat|PASSIVE|FISHING_SPEED=3||
treasure_hook|Treasure Hook|5|fishing|FISHING_ROD|UNCOMMON|Increases Fishing Fortune by 2 per level.|stat|PASSIVE|FISHING_FORTUNE=2||
lucky_cast|Lucky Cast|5|fishing|FISHING_ROD|RARE|Increases treasure chance by 1% per level.|stat|PASSIVE|TREASURE_CHANCE=1||
steady_line|Steady Line|5|fishing|FISHING_ROD|RARE|Increases fishing bar size by 2 per level.|steady_line|FISHING_MINIGAME|||
quick_reel|Quick Reel|5|fishing|FISHING_ROD|RARE|Increases fishing progress by 2 per level.|quick_reel|FISHING_MINIGAME|||
river_blessing|River Blessing|5|fishing|FISHING_ROD|EPIC|Increases custom fish value by 3% per level.|river_blessing|FISH_CAUGHT|||
ocean_bounty|Ocean Bounty|5|fishing|FISHING_ROD|EPIC|May catch a second fish.|ocean_bounty|FISH_CAUGHT|||
bait_saver|Bait Saver|5|fishing|FISHING_ROD|RARE|May preserve consumed bait.|bait_saver|FISH_CAST|||
golden_hook|Golden Hook|5|fishing|FISHING_ROD|EPIC|Increases the chance to catch rare fish.|golden_hook|FISH_CAUGHT|||
calm_cast|Calm Cast|5|fishing|FISHING_ROD|RARE|Slows fish movement in the fishing minigame.|calm_cast|FISHING_MINIGAME|||
deep_miner|Deep Miner|5|tool|PICKAXE,DRILL|UNCOMMON|Increases Mining Speed by 25 per level.|stat|PASSIVE|MINING_SPEED=25||
ore_fortune|Ore Fortune|5|tool|PICKAXE,DRILL|RARE|Increases Mining Fortune by 10 per level.|stat|PASSIVE|MINING_FORTUNE=10||
gemstone_touch|Gemstone Touch|5|tool|PICKAXE,DRILL|EPIC|May produce bonus gem drops.|gemstone_touch|BLOCK_BREAK|||
tunnel_focus|Tunnel Focus|5|tool|PICKAXE,DRILL|UNCOMMON|Increases Mining Speed underground.|tunnel_focus|BLOCK_BREAK|MINING_SPEED=5||
compacting_force|Compacting Force|5|tool|PICKAXE,DRILL|RARE|May compact or duplicate an ore drop.|compacting_force|BLOCK_BREAK|||
crystal_breaker|Crystal Breaker|5|tool|PICKAXE,DRILL|RARE|Improves mining effects against crystal and ore nodes.|crystal_breaker|BLOCK_BREAK|||
smelters_touch|Smelter's Touch|3|tool|PICKAXE,DRILL|EPIC|May automatically smelt broken ores.|smelters_touch|BLOCK_BREAK|none|silk_touch|
sturdy_handle|Sturdy Handle|5|tool|PICKAXE,DRILL|UNCOMMON|Increases Mining Fortune by 5 per level.|stat|PASSIVE|MINING_FORTUNE=5||
treasure_vein|Treasure Vein|5|tool|PICKAXE,DRILL|EPIC|May uncover bonus mining treasure.|treasure_vein|BLOCK_BREAK|TREASURE_CHANCE=1||
cave_blessing|Cave Blessing|5|tool|PICKAXE,DRILL|RARE|Increases mining experience gains by 2% per level.|cave_blessing|BLOCK_BREAK|XP_BOOST=2||
timber_blessing|Timber Blessing|5|tool|AXE|UNCOMMON|Increases Foraging Fortune by 10 per level.|stat|PASSIVE|FORAGING_FORTUNE=10||
bark_ripper|Bark Ripper|5|tool|AXE|UNCOMMON|Increases foraging speed by 20 per level.|bark_ripper|BLOCK_BREAK|||
root_finder|Root Finder|5|tool|AXE|RARE|May produce bonus saplings or foraging drops.|root_finder|BLOCK_BREAK|||
soil_master|Soil Master|5|tool|SHOVEL|UNCOMMON|Increases digging speed by 20 per level.|soil_master|BLOCK_BREAK|||
clay_fortune|Clay Fortune|5|tool|SHOVEL|RARE|Increases Digging Fortune by 10 per level.|stat|PASSIVE|EXCAVATION_FORTUNE=10||
harvest_blessing|Harvest Blessing|5|tool|HOE,FARMING_HOE|UNCOMMON|Increases Farming Fortune by 10 per level.|stat|PASSIVE|FARMING_FORTUNE=10||
crop_wisdom|Crop Wisdom|5|tool|HOE,FARMING_HOE|RARE|Increases farming experience gains by 2% per level.|crop_wisdom|BLOCK_BREAK|FARMING_XP_BOOST=2||
seed_saver|Seed Saver|5|tool|HOE,FARMING_HOE|RARE|May produce extra seeds from crops.|seed_saver|BLOCK_BREAK|||
golden_harvest|Golden Harvest|5|tool|HOE,FARMING_HOE|EPIC|May produce bonus crop drops.|golden_harvest|BLOCK_BREAK|||
replant_touch|Replant Touch|3|tool|HOE,FARMING_HOE|EPIC|Replants mature crops after harvesting.|replant_touch|BLOCK_BREAK|||
growth_custom|Growth|5|armor|ARMOR|UNCOMMON|Increases Health by 20 per level.|stat|PASSIVE|HEALTH=20||
guardian_custom|Guardian|5|armor|ARMOR|UNCOMMON|Increases Defense by 8 per level.|stat|PASSIVE|DEFENSE=8||
mana_weave|Mana Weave|5|armor|ARMOR|RARE|Increases Mana by 25 per level.|stat|PASSIVE|MANA=25||
vitality_custom|Vitality|5|armor|ARMOR|RARE|Increases healing received by 2% per level.|stat|PASSIVE|VITALITY=2||
rejuvenate_custom|Rejuvenate|5|armor|ARMOR|EPIC|Regenerates a portion of maximum health periodically.|rejuvenate_custom|PASSIVE_TICK|||
thorns_custom|Thorns|5|armor|ARMOR|RARE|Reflects 1% of incoming damage per level.|thorns_custom|TAKE_DAMAGE|||
dungeon_guard|Dungeon Guard|5|armor|ARMOR|EPIC|Increases Defense inside dungeons.|dungeon_guard|TAKE_DAMAGE|||
fire_ward|Fire Ward|4|armor|ARMOR|RARE|Reduces fire damage by 5% per level.|fire_ward|TAKE_DAMAGE|||
poison_ward|Poison Ward|4|armor|ARMOR|RARE|Reduces poison damage by 5% per level.|poison_ward|TAKE_DAMAGE|||
frost_ward|Frost Ward|4|armor|ARMOR|RARE|Reduces freezing and slowness duration.|frost_ward|PASSIVE_TICK|||
clear_mind|Clear Mind|5|armor|HELMET|RARE|Increases Mana by 20 per level.|stat|PASSIVE|MANA=20||
sharp_vision|Sharp Vision|5|armor|HELMET|RARE|Increases Crit Chance by 2 per level.|stat|PASSIVE|CRIT_CHANCE=2||
night_hunter|Night Hunter|3|armor|HELMET|EPIC|Grants night vision while worn.|night_hunter|PASSIVE_TICK|none|nightcloak|
iron_heart|Iron Heart|5|armor|CHESTPLATE|RARE|Increases Health by 30 per level.|stat|PASSIVE|HEALTH=30||
plated_core|Plated Core|5|armor|CHESTPLATE|RARE|Increases Defense by 12 per level.|stat|PASSIVE|DEFENSE=12||
battle_guard|Battle Guard|5|armor|CHESTPLATE|EPIC|Reduces damage received from mobs by 2% per level.|battle_guard|TAKE_DAMAGE|||
agile_steps|Agile Steps|5|armor|LEGGINGS|RARE|Increases Speed by 2 per level.|stat|PASSIVE|SPEED=2||
sturdy_legs|Sturdy Legs|5|armor|LEGGINGS|RARE|Increases Defense by 10 per level.|stat|PASSIVE|DEFENSE=10||
long_stride|Long Stride|5|armor|LEGGINGS|EPIC|Increases movement speed while worn.|long_stride|PASSIVE_TICK|||
feather_step|Feather Step|5|armor|BOOTS|RARE|Reduces fall damage by 8% per level.|feather_step|TAKE_DAMAGE|||
swift_boots|Swift Boots|5|armor|BOOTS|RARE|Increases Speed by 3 per level.|stat|PASSIVE|SPEED=3||
jump_boost_custom|Jump Boost|3|armor|BOOTS|EPIC|Grants jump boost while worn.|jump_boost_custom|PASSIVE_TICK|||
spell_power|Spell Power|5|magic|WAND,STAFF|RARE|Increases Ability Damage by 10 per level.|stat|PASSIVE|ABILITY_DAMAGE=10||
mana_surge|Mana Surge|5|magic|WAND,STAFF|RARE|Increases Mana by 30 per level.|stat|PASSIVE|MANA=30||
cooldown_flow|Cooldown Flow|5|magic|WAND,STAFF|EPIC|Reduces ability cooldowns by 1% per level.|cooldown_flow|ABILITY_CAST|||
arcane_focus|Arcane Focus|5|magic|WAND,STAFF|EPIC|Reduces ability mana cost by 1% per level.|arcane_focus|ABILITY_CAST|||
healing_light|Healing Light|5|magic|WAND,STAFF|EPIC|Increases healing ability power by 5% per level.|healing_light|ABILITY_CAST|||
frost_magic|Frost Magic|5|magic|WAND,STAFF|EPIC|Increases frost ability damage by 4% per level.|frost_magic|ABILITY_CAST|||
fire_magic|Fire Magic|5|magic|WAND,STAFF|EPIC|Increases fire ability damage by 4% per level.|fire_magic|ABILITY_CAST|||
shadow_magic|Shadow Magic|5|magic|WAND,STAFF|EPIC|Increases dark ability damage by 4% per level.|shadow_magic|ABILITY_CAST|||
lucky_charm|Lucky Charm|5|accessory|ACCESSORY|RARE|Increases Magic Find by 2 per level.|stat|PASSIVE|MAGIC_FIND=2||
coin_blessing|Coin Blessing|5|accessory|ACCESSORY|EPIC|Increases coin gains by 1% per level.|coin_blessing|COIN_GAIN|||
experience_charm|Experience Charm|5|accessory|ACCESSORY|EPIC|Increases skill experience gains by 2% per level.|stat|PASSIVE|XP_BOOST=2||
dungeon_luck|Dungeon Luck|5|accessory|ACCESSORY|EPIC|Increases dungeon loot luck by 2 per level.|dungeon_luck|DUNGEON_LOOT|||
potion_boost|Potion Boost|5|utility|ACCESSORY,UTILITY|RARE|Increases potion duration by 3% per level.|potion_boost|POTION_APPLY|||
hungry_soul|Hungry Soul|5|utility|ACCESSORY,UTILITY|RARE|Increases food healing by 2% per level.|hungry_soul|FOOD_CONSUME|||
storage_blessing|Storage Blessing|3|utility|UTILITY|EPIC|Provides a storage utility bonus when supported.|disabled|DISABLED|||false
safe_landing|Safe Landing|5|utility|ACCESSORY,UTILITY|RARE|Reduces fall damage by 5% per level.|safe_landing|TAKE_DAMAGE|||
explorer|Explorer|5|accessory|ACCESSORY|EPIC|Increases Speed and Magic Find outside dungeons.|stat|PASSIVE|SPEED=2,MAGIC_FIND=1||
survivor|Survivor|5|accessory|ACCESSORY|EPIC|Grants Health and Defense while below 40% health.|survivor|PASSIVE|HEALTH=10,DEFENSE=4||
""";

    private CustomEnchantDefaults100() {
    }

    public static List<Definition> definitions() {
        return ParsedHolder.RESULT.definitions();
    }

    public static List<InvalidDefinition> invalidDefinitions() {
        return ParsedHolder.RESULT.invalidDefinitions();
    }

    public static int sourceDefinitionCount() {
        return (int) DEFINITIONS.lines().filter(value -> !value.isBlank()).count();
    }

    private static ParseResult parseDefinitions() {
        List<Definition> result = new ArrayList<>();
        List<InvalidDefinition> invalid = new ArrayList<>();
        Set<String> ids = new LinkedHashSet<>();
        for (String line : DEFINITIONS.lines().filter(value -> !value.isBlank()).toList()) {
            String[] parts = line.split("\\|", -1);
            for (int index = 0; index < parts.length; index++) parts[index] = parts[index].trim();
            String id = parts.length == 0 || parts[0].isBlank() ? "<unknown>" : normalize(parts[0]);
            try {
                if (parts.length < 9 || parts.length > 12) throw new IllegalArgumentException("expected 9-12 fields but found " + parts.length);
                for (int index = 0; index < 9; index++) if (parts[index].isBlank()) throw new IllegalArgumentException("required field " + (index + 1) + " is empty");
                int maxLevel;
                try {
                    maxLevel = Integer.parseInt(parts[2]);
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("max level is not an integer");
                }
                if (maxLevel < 1 || maxLevel > 10) throw new IllegalArgumentException("max level must be between 1 and 10");
                Set<String> allowedTypes = splitSet(parts[4]);
                if (allowedTypes.isEmpty()) throw new IllegalArgumentException("allowed item types are empty");
                for (String type : allowedTypes) if (!VALID_ITEM_TYPES.contains(type)) throw new IllegalArgumentException("unknown item type " + type);
                String rarity = parts[5].toUpperCase(Locale.ROOT);
                if (!VALID_RARITIES.contains(rarity)) throw new IllegalArgumentException("unknown rarity " + parts[5]);
                String trigger = parts[8].toUpperCase(Locale.ROOT);
                if (!VALID_TRIGGERS.contains(trigger)) throw new IllegalArgumentException("unknown trigger " + parts[8]);
                if (!ids.add(id)) throw new IllegalArgumentException("duplicate enchant ID");
                String statField = optional(parts, 9);
                String conflictField = optional(parts, 10);
                String enabledField = optional(parts, 11);
                if (!enabledField.isBlank() && !enabledField.equalsIgnoreCase("true") && !enabledField.equalsIgnoreCase("false")) throw new IllegalArgumentException("enabled flag must be true or false");
                result.add(new Definition(id, parts[1], maxLevel, normalize(parts[3]), allowedTypes, rarity, parts[6],
                        normalize(parts[7]), trigger, stats(statField), splitSet(noneToEmpty(conflictField)),
                        enabledField.isBlank() || Boolean.parseBoolean(enabledField)));
            } catch (IllegalArgumentException exception) {
                invalid.add(new InvalidDefinition(id, exception.getMessage()));
            }
        }
        Set<String> validIds = result.stream().map(Definition::id).map(value -> value.toUpperCase(Locale.ROOT)).collect(java.util.stream.Collectors.toSet());
        List<Definition> conflictValid = new ArrayList<>();
        for (Definition definition : result) {
            String unknown = definition.conflicts().stream().filter(conflict -> !validIds.contains(conflict) && !ALLOWED_EXTERNAL_CONFLICTS.contains(conflict)).findFirst().orElse(null);
            if (unknown == null) conflictValid.add(definition);
            else invalid.add(new InvalidDefinition(definition.id(), "unknown conflict reference " + unknown));
        }
        result = conflictValid;
        return new ParseResult(List.copyOf(result), List.copyOf(invalid));
    }

    public static Set<String> ids() {
        Set<String> ids = new LinkedHashSet<>();
        definitions().forEach(definition -> ids.add(definition.id()));
        return Set.copyOf(ids);
    }

    private static Set<String> splitSet(String value) {
        if (value == null || value.isBlank()) return Set.of();
        Set<String> values = new LinkedHashSet<>();
        for (String part : value.split(",")) {
            String normalized = part.trim().toUpperCase(Locale.ROOT);
            if (!normalized.isBlank()) values.add(normalized);
        }
        return Set.copyOf(values);
    }

    private static Map<MMOStatType, Double> stats(String value) {
        value = noneToEmpty(value);
        if (value.isBlank()) return Map.of();
        Map<MMOStatType, Double> stats = new LinkedHashMap<>();
        for (String entry : value.split(",")) {
            String[] pair = entry.split("=", 2);
            MMOStatType type = pair.length == 2 ? MMOStatType.parse(pair[0]) : null;
            if (type == null) throw new IllegalArgumentException("unknown stat modifier " + entry.trim());
            double amount;
            try {
                amount = Double.parseDouble(pair[1].trim());
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("invalid stat modifier " + entry.trim());
            }
            if (!Double.isFinite(amount) || amount <= 0.0D) throw new IllegalArgumentException("invalid stat modifier " + entry.trim());
            stats.put(type, amount);
        }
        return Map.copyOf(stats);
    }

    private static String optional(String[] parts, int index) {
        return index < parts.length ? parts[index].trim() : "";
    }

    private static String noneToEmpty(String value) {
        return value == null || value.isBlank() || value.equalsIgnoreCase("none") ? "" : value.trim();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private static final Set<String> VALID_ITEM_TYPES = Set.of("SWORD", "KATANA", "DAGGER", "GREATSWORD", "BOW", "SHORTBOW", "LONGBOW", "CROSSBOW", "FISHING_ROD", "PICKAXE", "DRILL", "AXE", "FORAGING_AXE", "SHOVEL", "HOE", "FARMING_HOE", "ARMOR", "HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS", "WAND", "STAFF", "RING", "NECKLACE", "CHARM", "TOTEM", "ACCESSORY", "UTILITY");
    private static final Set<String> VALID_RARITIES = Set.of("COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC");
    private static final Set<String> VALID_TRIGGERS = Set.of("PASSIVE", "ON_HIT", "PROJECTILE_HIT", "PROJECTILE_LAUNCH", "KILL_ENTITY", "FISHING_MINIGAME", "FISH_CAUGHT", "FISH_CAST", "BLOCK_BREAK", "PASSIVE_TICK", "TAKE_DAMAGE", "ABILITY_CAST", "COIN_GAIN", "DUNGEON_LOOT", "POTION_APPLY", "FOOD_CONSUME", "DISABLED");
    private static final Set<String> ALLOWED_EXTERNAL_CONFLICTS = Set.of("SILK_TOUCH", "NIGHTCLOAK");

    private static final class ParsedHolder {
        private static final ParseResult RESULT = parseDefinitions();
    }

    private record ParseResult(List<Definition> definitions, List<InvalidDefinition> invalidDefinitions) {
    }

    public record InvalidDefinition(String id, String reason) {
    }

    public record Definition(String id, String name, int maxLevel, String type, Set<String> allowedItemTypes,
                             String rarity, String description, String effect, String trigger,
                             Map<MMOStatType, Double> statBonuses, Set<String> conflicts, boolean enabled) {
    }
}
