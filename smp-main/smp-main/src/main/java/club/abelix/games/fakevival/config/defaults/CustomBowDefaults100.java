package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class CustomBowDefaults100 {
    private static final String IDS = """
            worn_training_bow
            chipped_oak_bow
            village_archer_bow
            farmhand_bow
            river_reed_bow
            dusty_hunter_bow
            beginner_guard_bow
            old_miner_bow
            mossy_forest_bow
            cobbled_string_bow
            hayvillage_shortbow
            wooden_scout_shortbow
            cracked_arrow_shortbow
            fisher_watch_shortbow
            barn_roof_shortbow
            simple_stone_longbow
            beginner_longbow
            guard_tower_longbow
            rusty_crossbow
            old_watch_crossbow
            reinforced_oak_bow
            bronze_hunter_bow
            ironleaf_bow
            desert_nomad_bow
            baywatch_bow
            riverfern_bow
            froststring_bow
            swampbone_bow
            cherry_guard_bow
            grainfield_bow
            copper_quickdraw_shortbow
            ember_shortbow
            hunter_fang_shortbow
            wolftrack_shortbow
            mushroom_spore_shortbow
            steelwood_longbow
            desert_tower_longbow
            forest_ranger_longbow
            copper_crossbow
            bandit_crossbow
            royal_guard_bow
            imperial_soldier_bow
            purple_arcane_bow
            frostbite_bow
            blazing_desert_bow
            drowned_bay_bow
            river_spirit_bow
            forest_ranger_bow
            swamp_cursed_bow
            cherry_blossom_bow
            mansion_duelist_shortbow
            palm_temple_shortbow
            grain_reaper_shortbow
            shadowfang_shortbow
            crystal_guardian_shortbow
            molten_iron_longbow
            venom_edge_longbow
            moonlit_steel_longbow
            cavebreaker_crossbow
            enchanted_knight_crossbow
            royal_champion_bow
            imperial_officer_bow
            arcane_spellbow
            frozen_warden_bow
            sunfire_bow
            tidecaller_bow
            ancient_river_bow
            forest_elder_bow
            bogreaper_bow
            sakura_spirit_bow
            mansion_secret_shortbow
            temple_relic_shortbow
            golden_grain_shortbow
            voidfang_shortbow
            stormforged_shortbow
            magma_core_shortbow
            toxic_venom_longbow
            lunar_guard_longbow
            deep_cavern_crossbow
            enchanted_royal_crossbow
            kings_oath_bow
            imperial_warlord_bow
            purple_void_spellbow
            glacial_titan_bow
            solar_pharaoh_bow
            abyssal_tide_bow
            river_deity_bow
            ancient_forest_bow
            cursed_swamp_reaver_bow
            sakura_celestial_bow
            mansion_ghost_shortbow
            relic_sun_shortbow
            harvest_lord_shortbow
            dragonsteel_longbow
            thunderking_crossbow
            bow_of_the_false_king
            imperial_eternity_shortbow
            void_sakura_shortbow
            primordial_dragon_longbow
            celestial_worldbreaker_crossbow
            """;
    private static final Set<String> SHORTBOWS = Set.of(
            "wooden_scout_shortbow", "cracked_arrow_shortbow", "fisher_watch_shortbow", "barn_roof_shortbow",
            "copper_quickdraw_shortbow", "ember_shortbow", "hunter_fang_shortbow", "wolftrack_shortbow",
            "mushroom_spore_shortbow", "mansion_duelist_shortbow", "palm_temple_shortbow", "grain_reaper_shortbow",
            "shadowfang_shortbow", "crystal_guardian_shortbow", "mansion_secret_shortbow", "temple_relic_shortbow",
            "golden_grain_shortbow", "voidfang_shortbow", "stormforged_shortbow", "magma_core_shortbow",
            "mansion_ghost_shortbow", "relic_sun_shortbow", "harvest_lord_shortbow",
            "imperial_eternity_shortbow", "void_sakura_shortbow"
    );
    private static final Set<String> LONGBOWS = Set.of(
            "beginner_longbow", "guard_tower_longbow", "desert_tower_longbow", "forest_ranger_longbow",
            "venom_edge_longbow", "moonlit_steel_longbow", "toxic_venom_longbow", "lunar_guard_longbow",
            "dragonsteel_longbow", "primordial_dragon_longbow"
    );
    private static final Set<String> CROSSBOWS = Set.of(
            "enchanted_knight_crossbow", "deep_cavern_crossbow", "enchanted_royal_crossbow",
            "thunderking_crossbow", "celestial_worldbreaker_crossbow"
    );

    private CustomBowDefaults100() {
    }

    public static void apply(ConfigurationSection root) {
        for (Definition definition : definitions()) {
            String base = "custom-items.items." + definition.id();
            root.set(base, null);
            root.set(base + ".name", rarityColor(definition.rarity()) + definition.name());
            root.set(base + ".material", definition.material());
            root.set(base + ".type", definition.type());
            root.set(base + ".mmo.type", definition.type());
            root.set(base + ".category", "weapons");
            root.set(base + ".subcategory", "bows");
            root.set(base + ".item-category", "WEAPONS");
            root.set(base + ".item-subcategory", "BOWS");
            root.set(base + ".rarity", definition.rarity());
            root.set(base + ".lore", List.of(definition.lore().split("~", -1)));
            root.set(base + ".mmo.stats.DAMAGE", definition.damage());
            root.set(base + ".mmo.stats.STRENGTH", definition.strength());
            root.set(base + ".mmo.stats.CRIT_CHANCE", definition.critChance());
            root.set(base + ".mmo.stats.CRIT_DAMAGE", definition.critDamage());
            root.set(base + ".mmo.stats.PROJECTILE_DAMAGE", definition.projectileDamage());
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
    }

    public static List<Definition> definitions() {
        List<String> ids = IDS.lines().map(String::trim).filter(value -> !value.isBlank()).toList();
        List<Definition> result = new ArrayList<>();
        int[] rarityIndexes = new int[6];
        for (int index = 0; index < ids.size(); index++) {
            String id = ids.get(index);
            int rarityIndex = rarityIndex(index);
            String rarity = rarity(rarityIndex);
            int withinTier = rarityIndexes[rarityIndex]++;
            int tierSize = rarityIndex == 4 ? 15 : rarityIndex == 5 ? 5 : 20;
            String type = type(id);
            String name = pretty(id);
            result.add(new Definition(id, name, type.equals("CROSSBOW") ? "CROSSBOW" : "BOW", type, rarity,
                    scale(rarityIndex, withinTier, tierSize, 0), scale(rarityIndex, withinTier, tierSize, 1),
                    scale(rarityIndex, withinTier, tierSize, 2), scale(rarityIndex, withinTier, tierSize, 3),
                    scale(rarityIndex, withinTier, tierSize, 4), playerLevel(rarityIndex, withinTier, tierSize),
                    lore(id, name)));
        }
        if (result.size() != 100) throw new IllegalStateException("Custom bow batch must contain 100 definitions");
        if (result.stream().map(Definition::id).collect(java.util.stream.Collectors.toSet()).size() != 100)
            throw new IllegalStateException("Custom bow batch contains duplicate IDs");
        if (result.stream().filter(definition -> definition.type().equals("BOW")).count() != 60
                || result.stream().filter(definition -> definition.type().equals("SHORTBOW")).count() != 25
                || result.stream().filter(definition -> definition.type().equals("LONGBOW")).count() != 10
                || result.stream().filter(definition -> definition.type().equals("CROSSBOW")).count() != 5)
            throw new IllegalStateException("Custom bow type spread is invalid");
        return List.copyOf(result);
    }

    public static Set<String> itemIds() {
        Set<String> ids = new LinkedHashSet<>();
        definitions().forEach(definition -> ids.add(definition.id()));
        return Set.copyOf(ids);
    }

    private static int rarityIndex(int index) {
        if (index < 20) return 0;
        if (index < 40) return 1;
        if (index < 60) return 2;
        if (index < 80) return 3;
        if (index < 95) return 4;
        return 5;
    }

    private static String rarity(int index) {
        return List.of("COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC").get(index);
    }

    private static String type(String id) {
        if (SHORTBOWS.contains(id)) return "SHORTBOW";
        if (LONGBOWS.contains(id)) return "LONGBOW";
        if (CROSSBOWS.contains(id)) return "CROSSBOW";
        return "BOW";
    }

    private static int scale(int rarity, int index, int count, int stat) {
        int[][] minimum = {
                {8, 1, 1, 1, 1}, {25, 8, 2, 10, 8}, {60, 25, 4, 25, 20},
                {120, 60, 7, 60, 50}, {240, 120, 10, 120, 100}, {450, 240, 15, 240, 200}
        };
        int[][] maximum = {
                {30, 12, 3, 12, 10}, {65, 28, 5, 30, 25}, {130, 65, 8, 70, 55},
                {260, 135, 12, 140, 110}, {460, 260, 18, 260, 220}, {750, 420, 25, 420, 360}
        };
        return minimum[rarity][stat] + (int) Math.round((maximum[rarity][stat] - minimum[rarity][stat]) * (index / (double) Math.max(1, count - 1)));
    }

    private static int playerLevel(int rarity, int index, int count) {
        int[] minimum = {1, 250, 1000, 2500, 5000, 8000};
        int[] maximum = {250, 1000, 2500, 5000, 8000, 10000};
        return minimum[rarity] + (int) Math.round((maximum[rarity] - minimum[rarity]) * (index / (double) Math.max(1, count - 1)));
    }

    private static String lore(String id, String name) {
        String theme = id.contains("frost") || id.contains("glacial") || id.contains("frozen") ? "winter silence"
                : id.contains("river") || id.contains("tide") || id.contains("bay") || id.contains("drowned") ? "restless water"
                : id.contains("forest") || id.contains("oak") || id.contains("moss") || id.contains("ironleaf") ? "deep woodland"
                : id.contains("desert") || id.contains("solar") || id.contains("sun") ? "unbroken sunlight"
                : id.contains("void") || id.contains("shadow") || id.contains("ghost") ? "starless darkness"
                : id.contains("dragon") ? "ancient dragonfire"
                : id.contains("sakura") || id.contains("cherry") ? "falling blossoms"
                : id.contains("storm") || id.contains("thunder") ? "gathering thunder"
                : id.contains("swamp") || id.contains("bog") ? "whispering marshland"
                : id.contains("imperial") || id.contains("royal") || id.contains("king") ? "forgotten crowns"
                : id.contains("magma") || id.contains("ember") || id.contains("blazing") ? "living flame"
                : "old frontier trails";
        String flight = id.contains("shortbow") ? "leaps from the string before an enemy can brace"
                : id.contains("longbow") ? "holds a steady course across impossible distance"
                : id.contains("crossbow") ? "strikes with the certainty of a locked mechanism"
                : "follows the archer's aim through wind and weather";
        return "&7The " + name + " carries the memory of " + theme + ".~&7Each arrow " + flight + ".";
    }

    private static String pretty(String id) {
        StringBuilder result = new StringBuilder();
        for (String part : id.split("_")) {
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return result.toString().replace("Kings Oath", "King's Oath");
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

    public record Definition(String id, String name, String material, String type, String rarity, int damage,
                             int strength, int critChance, int critDamage, int projectileDamage,
                             int playerLevel, String lore) {
    }
}
