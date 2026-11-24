package club.abelix.games.fakevival.mmo;

public enum MMOStatType {
    DAMAGE("Damage", "&c", false),
    PROJECTILE_DAMAGE("Projectile Damage", "&c", false),
    STRENGTH("Strength", "&c", false),
    CRIT_CHANCE("Crit Chance", "&c", true),
    CRIT_DAMAGE("Crit Damage", "&c", true),
    FEROCITY("Ferocity", "&c", false),
    LIFESTEAL("Lifesteal", "&c", true),
    HEALTH("Health", "&a", false),
    MANA("Mana", "&b", false),
    MANA_REGEN("Mana Regen", "&b", false),
    INTELLIGENCE("Intelligence", "&b", false),
    ABILITY_DAMAGE("Ability Damage", "&d", true),
    DEFENSE("Defense", "&a", false),
    TRUE_DEFENSE("True Defense", "&a", false),
    SPEED("Speed", "&a", false),
    VITALITY("Vitality", "&a", true),
    MINING_SPEED("Mining Speed", "&6", false),
    MINING_FORTUNE("Mining Fortune", "&6", false),
    BREAKING_POWER("Breaking Power", "&6", false),
    FORAGING_SPEED("Foraging Speed", "&6", false),
    FORAGING_FORTUNE("Foraging Fortune", "&6", false),
    DIGGING_SPEED("Digging Speed", "&6", false),
    DIGGING_FORTUNE("Digging Fortune", "&6", false),
    EXCAVATION_FORTUNE("Excavation Fortune", "&6", false),
    FARMING_FORTUNE("Farming Fortune", "&6", false),
    REPLANT_CHANCE("Replant Chance", "&6", true),
    CROP_GROWTH("Crop Growth", "&6", false),
    HARVEST_RANGE("Harvest Range", "&6", false),
    DOUBLE_DROP_CHANCE("Double Drop Chance", "&6", true),
    FARMING_XP_BOOST("Farming XP Boost", "&6", true),
    FISHING_SPEED("Fishing Speed", "&b", false),
    FISHING_FORTUNE("Fishing Fortune", "&b", false),
    FISHING_BAR_SIZE("Fishing Bar Size", "&b", false),
    FISHING_PROGRESS_BONUS("Fishing Progress Bonus", "&b", false),
    SEA_CREATURE_CHANCE("Sea Creature Chance", "&b", true),
    TREASURE_CHANCE("Treasure Chance", "&b", true),
    FISHING_XP_BOOST("Fishing XP Boost", "&b", true),
    MAGIC_FIND("Magic Find", "&d", false),
    PET_LUCK("Pet Luck", "&d", false),
    XP_BOOST("XP Boost", "&d", true),
    ALCHEMY_XP_BOOST("Alchemy XP Boost", "&d", true),
    ENCHANTING_XP_BOOST("Enchanting XP Boost", "&d", true),
    REPAIR_XP_BOOST("Repair XP Boost", "&d", true),
    SMELTING_XP_BOOST("Smelting XP Boost", "&6", true),
    HEALTH_RESTORE("Health Restore", "&c", false),
    MANA_RESTORE("Mana Restore", "&b", false);

    private final String display;
    private final String color;
    private final boolean percent;

    MMOStatType(String display, String color, boolean percent) {
        this.display = display;
        this.color = color;
        this.percent = percent;
    }

    public String display() {
        return display;
    }

    public String color() {
        return color;
    }

    public boolean percent() {
        return percent;
    }

    public static MMOStatType parse(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT).replace('-', '_').replace(' ', '_'));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
