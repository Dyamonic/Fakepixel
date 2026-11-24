package club.abelix.games.fakevival.recipes;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class RecipeUnlock {
    public enum Type {
        DEFAULT,
        COLLECTION,
        MULTI_COLLECTION,
        SKILL_LEVEL,
        SKILL_XP,
        COLLECT_MATERIAL,
        COLLECTION_TIER,
        ADVENTURE_RANK,
        DUNGEON_COMPLETION,
        BOSS_KILL,
        PLAYER_LEVEL,
        QUEST,
        PERMISSION,
        ADMIN_ONLY
    }

    private final Type type;
    private final String collectionId;
    private final int tier;
    private final Map<String, Integer> requirements;
    private final String skill;
    private final int level;
    private final String quest;
    private final String period;
    private final String permission;

    public RecipeUnlock(Type type, String collectionId, int tier, Map<String, Integer> requirements, String skill, int level, String quest, String period, String permission) {
        this.type = type == null ? Type.DEFAULT : type;
        this.collectionId = normalize(collectionId);
        this.tier = Math.max(1, tier);
        this.requirements = Map.copyOf(requirements == null ? Map.of() : requirements);
        this.skill = normalize(skill);
        this.level = Math.max(1, level);
        this.quest = normalize(quest);
        this.period = normalize(period);
        this.permission = permission == null ? "" : permission.trim();
    }

    public static RecipeUnlock defaultUnlock() {
        return new RecipeUnlock(Type.DEFAULT, "", 1, Map.of(), "", 1, "", "", "");
    }

    public Type type() {
        return type;
    }

    public String collectionId() {
        return collectionId;
    }

    public int tier() {
        return tier;
    }

    public Map<String, Integer> requirements() {
        return requirements;
    }

    public String skill() {
        return skill;
    }

    public int level() {
        return level;
    }

    public String quest() {
        return quest;
    }

    public String period() {
        return period;
    }

    public String permission() {
        return permission;
    }

    public static Type parseType(String value) {
        if (value == null || value.isBlank()) return Type.DEFAULT;
        try {
            return Type.valueOf(value.toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_'));
        } catch (IllegalArgumentException ignored) {
            return Type.DEFAULT;
        }
    }

    public static Map<String, Integer> normalizeRequirements(Map<String, Integer> requirements) {
        Map<String, Integer> normalized = new LinkedHashMap<>();
        if (requirements != null) {
            requirements.forEach((key, value) -> normalized.put(normalize(key), Math.max(1, value == null ? 1 : value)));
        }
        return normalized;
    }

    private static String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replace("-", "_").replace(" ", "_").trim();
    }
}
