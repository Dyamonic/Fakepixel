package me.outerspace.thepit.registry;

import me.outerspace.thepit.registry.definition.AbelixRankDefinition;
import me.outerspace.thepit.registry.definition.RankCategory;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class AbelixRankRegistry extends RegistryNode {
    private final Map<String, AbelixRankDefinition> ranks;
    private final Map<String, String> aliases;
    public AbelixRankRegistry(Map<String, Object> values) {
        super(values);
        Map<String, AbelixRankDefinition> built = new LinkedHashMap<>();
        load(built, RankCategory.ADMIN_STAFF, "admin-staff");
        load(built, RankCategory.STAFF, "staff");
        load(built, RankCategory.PLAYER_RANK, "player-ranks");
        if (!built.containsKey("DEFAULT")) built.put("DEFAULT", new AbelixRankDefinition(RankCategory.PLAYER_RANK, "DEFAULT", Collections.<String>emptyList()));
        ranks = Collections.unmodifiableMap(built);
        Map<String, String> configuredAliases = new LinkedHashMap<>();
        for (String alias : keys("rank-aliases")) configuredAliases.put(normalize(alias), normalize(getString("rank-aliases." + alias)));
        aliases = Collections.unmodifiableMap(configuredAliases);
    }
    private void load(Map<String, AbelixRankDefinition> target, RankCategory category, String path) {
        for (String configured : keys("rank-detection." + path)) {
            String normalized = normalize(configured);
            target.put(normalized, new AbelixRankDefinition(category, normalized,
                    getStringList("rank-detection." + path + "." + configured + ".permissions")));
        }
    }
    public Optional<AbelixRankDefinition> getAbelixRank(String normalizedRank) { return Optional.ofNullable(ranks.get(normalizedRank)); }
    public AbelixRankDefinition resolve(String rawRank) {
        String normalized = normalize(rawRank);
        normalized = aliases.getOrDefault(normalized, normalized);
        return ranks.getOrDefault(normalized, ranks.get("DEFAULT"));
    }
    public Map<String, AbelixRankDefinition> all() { return ranks; }
    public static String normalize(String rank) {
        if (rank == null) return "DEFAULT";
        String value = rank.replaceAll("(?i)[&§][0-9A-FK-OR]", "").trim();
        while (value.length() >= 2 && value.startsWith("[") && value.endsWith("]")) value = value.substring(1, value.length() - 1).trim();
        value = value.toUpperCase(Locale.ENGLISH).replace("+", "_PLUS").replaceAll("[\\s-]+", "_")
                .replaceAll("[^A-Z0-9_]", "").replaceAll("_+", "_").replaceAll("^_|_$", "");
        return value.isEmpty() ? "DEFAULT" : value;
    }
}
