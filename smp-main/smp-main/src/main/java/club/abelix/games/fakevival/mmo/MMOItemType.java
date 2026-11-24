package club.abelix.games.fakevival.mmo;

public enum MMOItemType {
    WEAPON,
    SWORD,
    KATANA,
    DAGGER,
    SPEAR,
    HAMMER,
    AXE,
    BOW,
    CROSSBOW,
    STAFF,
    WAND,
    ARMOR,
    TOOL,
    SHIELD,
    RING,
    NECKLACE,
    TRINKET,
    FISHING_ROD,
    FISHING_BAIT,
    BAIT,
    COMPANION,
    FARMING_EQUIPMENT,
    ROD,
    ACCESSORY,
    CHARM,
    TOTEM,
    TOKEN,
    QUEST_ITEM,
    CURRENCY,
    KEY,
    LOOT_BOX,
    COSMETIC,
    DECORATION,
    FURNITURE,
    MISC,
    UTILITY,
    MATERIAL,
    COLLECTION_MATERIAL,
    RECIPE_MATERIAL,
    CONSUMABLE,
    FOOD,
    POTION;

    public boolean canRollRandomRarityEffects() {
        return switch (this) {
            case WEAPON, SWORD, KATANA, DAGGER, SPEAR, HAMMER, AXE, BOW, CROSSBOW, STAFF, WAND,
                    ARMOR, TOOL, FISHING_ROD, ROD, RING, NECKLACE,
                    TRINKET, ACCESSORY, CHARM, TOTEM -> true;
            default -> false;
        };
    }

    public static MMOItemType parse(String value) {
        if (value == null || value.isBlank()) return UTILITY;
        String normalized = value.trim().toUpperCase(java.util.Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (normalized.equals("ROD") || normalized.equals("FISHING")) return FISHING_ROD;
        if (normalized.equals("FISHINGBAIT") || normalized.equals("FISHING_BAIT")) return FISHING_BAIT;
        if (normalized.equals("COLLECTIONMATERIAL")) return COLLECTION_MATERIAL;
        if (normalized.equals("RECIPEMATERIAL")) return RECIPE_MATERIAL;
        if (normalized.equals("LOOTBOX")) return LOOT_BOX;
        if (normalized.equals("GREATSWORD")) return SWORD;
        if (normalized.equals("SHORTBOW") || normalized.equals("SHORT_BOW")
                || normalized.equals("LONGBOW") || normalized.equals("LONG_BOW")
                || normalized.equals("GREATBOW")) return BOW;
        if (normalized.equals("AXE_WEAPON")) return AXE;
        if (normalized.equals("PICKAXE") || normalized.equals("DRILL") || normalized.equals("FORAGING_AXE")
                || normalized.equals("SHOVEL") || normalized.equals("HOE") || normalized.equals("MULTI_TOOL")
                || normalized.equals("DUNGEON_TOOL")) return TOOL;
        if (normalized.equals("FARMING_HOE")) return FARMING_EQUIPMENT;
        if (normalized.equals("DUNGEON_SWORD")) return SWORD;
        if (normalized.equals("DUNGEON_DAGGER")) return DAGGER;
        if (normalized.equals("DUNGEON_KATANA")) return KATANA;
        if (normalized.equals("DUNGEON_BOW")) return BOW;
        if (normalized.equals("DUNGEON_WAND")) return WAND;
        if (normalized.equals("DUNGEON_STAFF")) return STAFF;
        if (normalized.equals("DUNGEON_ARMOR")) return ARMOR;
        if (normalized.equals("HELMET") || normalized.equals("CHESTPLATE") || normalized.equals("LEGGINGS") || normalized.equals("BOOTS")) return ARMOR;
        if (normalized.equals("RECIPE_FRAGMENT")) return MISC;
        if (normalized.equals("KEY_FRAGMENT")) return KEY;
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return UTILITY;
        }
    }
}
