package club.abelix.games.fakevival.config.defaults;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class CustomEnchantUnlockDefaults {
    private static final Map<String, UnlockRequirement> EXPLICIT = explicit();

    private CustomEnchantUnlockDefaults() {
    }

    public static UnlockRequirement requirement(String id, String rarity) {
        UnlockRequirement explicit = EXPLICIT.get(normalize(id));
        if (explicit != null) return explicit;
        return switch (rarity == null ? "" : rarity.toUpperCase()) {
            case "MYTHIC" -> new UnlockRequirement(35, 1000, Map.of(), Map.of(), "Boss", 10, true);
            case "LEGENDARY" -> new UnlockRequirement(25, 500, Map.of(), Map.of(), "Boss", 5, false);
            case "EPIC" -> new UnlockRequirement(15, 250, Map.of(), Map.of(), "", 0, false);
            case "RARE" -> new UnlockRequirement(8, 100, Map.of(), Map.of(), "", 0, false);
            case "UNCOMMON" -> new UnlockRequirement(3, 25, Map.of(), Map.of(), "", 0, false);
            default -> new UnlockRequirement(1, 1, Map.of(), Map.of(), "", 0, false);
        };
    }

    public static Map<String, CollectionReward> collectionRewards() {
        Map<String, CollectionReward> rewards = new LinkedHashMap<>();
        rewards.put("blood_drinker", new CollectionReward("rotten_flesh", 4));
        rewards.put("holy_strike", new CollectionReward("bone", 3));
        rewards.put("venom_tip", new CollectionReward("spider_eye", 4));
        rewards.put("deep_miner", new CollectionReward("cobblestone", 5));
        rewards.put("guardian_custom", new CollectionReward("raw_iron", 5));
        rewards.put("treasure_hook", new CollectionReward("fishing", 5));
        rewards.put("dungeon_bane", new CollectionReward("dungeon", 5));
        return Map.copyOf(rewards);
    }

    public static Set<String> bookUnlockIds() {
        return EXPLICIT.entrySet().stream().filter(entry -> entry.getValue().requiresBookUnlock())
                .map(Map.Entry::getKey).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static Map<String, UnlockRequirement> explicit() {
        Map<String, UnlockRequirement> values = new LinkedHashMap<>();
        values.put("blood_drinker", requirement(15, 150, "rotten_flesh", 4));
        values.put("holy_strike", requirement(12, 100, "bone", 3));
        values.put("venom_tip", requirement(15, 150, "spider_eye", 4));
        values.put("deep_miner", requirement(18, 250, "cobblestone", 5));
        values.put("guardian_custom", requirement(20, 300, "raw_iron", 5));
        values.put("treasure_hook", requirement(18, 250, "fishing", 5));
        values.put("dungeon_bane", new UnlockRequirement(25, 500, Map.of(), Map.of("dungeon", 5), "", 0, false));
        values.put("dragon_rend", new UnlockRequirement(30, 750, Map.of(), Map.of(), "Boss", 10, true));
        values.put("royal_cleave", new UnlockRequirement(25, 500, Map.of(), Map.of(), "Boss", 5, true));
        values.put("storm_arrow", new UnlockRequirement(22, 400, Map.of(), Map.of(), "", 0, true));
        values.put("cooldown_flow", new UnlockRequirement(28, 600, Map.of(), Map.of(), "", 0, true));
        values.put("arcane_focus", new UnlockRequirement(28, 600, Map.of(), Map.of(), "", 0, true));
        return Map.copyOf(values);
    }

    private static UnlockRequirement requirement(int enchanting, int playerLevel, String collection, int tier) {
        return new UnlockRequirement(enchanting, playerLevel, Map.of(collection, tier), Map.of(), "", 0, false);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(java.util.Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    public record UnlockRequirement(int requiredEnchantingLevel, int requiredPlayerLevel,
                                    Map<String, Integer> requiredCollections,
                                    Map<String, Integer> requiredDungeonCollection,
                                    String bossName, int requiredBossKills, boolean requiresBookUnlock) {
        public UnlockRequirement {
            requiredCollections = requiredCollections == null ? Map.of() : Map.copyOf(requiredCollections);
            requiredDungeonCollection = requiredDungeonCollection == null ? Map.of() : Map.copyOf(requiredDungeonCollection);
            bossName = bossName == null ? "" : bossName;
        }
    }

    public record CollectionReward(String collectionId, int tier) {
    }
}
