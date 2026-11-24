package club.abelix.games.fakevival.furniture;

import org.bukkit.Material;

public record FurnitureDefinition(String id, String packId, String displayName, Category category,
                                  String sourceId, String entityJson, Material previewMaterial,
                                  double price, String permission, boolean placeable,
                                  boolean playerBreakable, boolean spawnProtected) {
    public enum Category {
        CHAIRS, TABLES, SHELVES, LAMPS, PLANTS, FOOD_DRINKS, BEDS, DECORATIONS, OUTDOOR, FURNITURE, MISC;

        public static Category parse(String input) {
            if (input == null) return MISC;
            String key = input.trim().toUpperCase().replace('/', '_').replace(' ', '_').replace('-', '_');
            try { return valueOf(key); } catch (IllegalArgumentException ignored) { return MISC; }
        }

        public String displayName() {
            if (this == FOOD_DRINKS) return "Food/Drinks";
            String lower = name().toLowerCase();
            return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
        }
    }
}
