package club.abelix.games.fakevival.managers;

import org.bukkit.entity.EntityType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class DungeonMobTemplateRegistry {
    private static final Map<String, DungeonMobTemplate> TEMPLATES = new LinkedHashMap<>();

    static {
        register("husk", EntityType.HUSK, "Husk", "husk");
        register("zombie", EntityType.ZOMBIE, "Zombie", "zombie");
        register("skeleton", EntityType.SKELETON, "Skeleton", "skeleton");
        register("spider", EntityType.SPIDER, "Spider", "spider", "spiders");
        register("cave_spider", EntityType.CAVE_SPIDER, "Cave Spider", "cave_spider");
        register("guard", EntityType.HUSK, "Guard", "guard", "guards");
        register("knight", EntityType.HUSK, "Knight", "knight", "knights");
        register("minion", EntityType.ZOMBIE, "Minion", "minion", "minions");
        register("archer", EntityType.SKELETON, "Archer", "archer", "archers");
        register("pirate", EntityType.HUSK, "Pirate", "pirate", "pirates");
        register("pirate_archer", EntityType.SKELETON, "Pirate Archer", "archer");
        register("pirate_brute", EntityType.ZOMBIE, "Pirate Brute", "brute");
        register("temple_spirit", EntityType.ZOMBIE, "Temple Spirit", "mage");
        register("sacred_minion", EntityType.SKELETON, "Sacred Minion", "minion");
        register("guardian", EntityType.ZOMBIE, "Guardian", "guard");
        register("captain", EntityType.HUSK, "Captain", "captain");
        register("boss", EntityType.ZOMBIE, "Boss", "boss");
        register("scout", EntityType.PILLAGER, "Scout", "scout", "scouts");
        register("officer", EntityType.VINDICATOR, "Officer", "officer", "officers");
    }

    private DungeonMobTemplateRegistry() {
    }

    public static DungeonMobTemplate resolve(String value) {
        String normalized = normalize(value);
        DungeonMobTemplate registered = TEMPLATES.get(normalized);
        if (registered != null) return registered;
        for (Map.Entry<String, DungeonMobTemplate> entry : TEMPLATES.entrySet()) {
            String suffix = "_" + entry.getKey();
            if (!normalized.endsWith(suffix)) continue;
            String prefix = normalized.substring(0, normalized.length() - suffix.length());
            DungeonMobTemplate base = entry.getValue();
            return new DungeonMobTemplate(prefix + "_" + base.mobId(), base.entityType(),
                    pretty(prefix) + " " + base.displayName(), base.role(), base.defaultLevel(),
                    base.defaultHealth(), base.defaultDamage());
        }
        try {
            EntityType type = EntityType.valueOf(normalized.toUpperCase(Locale.ROOT));
            if (!type.isAlive()) return null;
            String display = pretty(normalized);
            return new DungeonMobTemplate(normalized, type, display, normalized, 1, 500.0D, 5.0D);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static DungeonMobTemplate resolveDefinition(String spawnId, String mobId, String entityType) {
        DungeonMobTemplate template = resolve(mobId);
        if (template != null) return template;
        template = resolve(withoutInternalSuffix(spawnId));
        if (template != null) return template;
        return resolve(entityType);
    }

    public static String cleanDisplayName(String value, DungeonMobTemplate fallback) {
        String cleaned = value == null ? "" : value.replaceAll("(?i)[&§][0-9A-FK-ORX]", "").trim();
        cleaned = withoutInternalSuffix(cleaned).replace('_', ' ').trim();
        DungeonMobTemplate resolved = resolve(cleaned);
        if (resolved != null) return resolved.displayName();
        if (cleaned.isBlank()) return fallback == null ? "Mob" : fallback.displayName();
        String[] words = cleaned.toLowerCase(Locale.ROOT).split("\\s+");
        StringBuilder display = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) continue;
            if (!display.isEmpty()) display.append(' ');
            display.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return display.isEmpty() && fallback != null ? fallback.displayName() : display.toString();
    }

    public static String withoutInternalSuffix(String value) {
        if (value == null) return "";
        return value.trim().replaceFirst("(?i)(?:[ _-]+)\\d{10,}$", "");
    }

    public static List<String> aliases() {
        return List.copyOf(TEMPLATES.keySet());
    }

    private static void register(String mobId, EntityType entityType, String displayName, String role, String... aliases) {
        DungeonMobTemplate template = new DungeonMobTemplate(mobId, entityType, displayName, role, 1, 500.0D, 5.0D);
        TEMPLATES.put(mobId, template);
        for (String alias : aliases) TEMPLATES.put(alias, template);
    }

    private static String normalize(String value) {
        return withoutInternalSuffix(value).toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private static String pretty(String value) {
        String[] words = value.toLowerCase(Locale.ROOT).split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    public record DungeonMobTemplate(String mobId, EntityType entityType, String displayName, String role,
                                     int defaultLevel, double defaultHealth, double defaultDamage) {
    }
}
