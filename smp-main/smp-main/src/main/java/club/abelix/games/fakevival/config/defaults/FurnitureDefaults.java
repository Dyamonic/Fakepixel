package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

public final class FurnitureDefaults {
    private FurnitureDefaults() {}

    public static void apply(ConfigurationSection root) {
        root.set("furniture.enabled", false);
        root.set("furniture.shop.enabled", false);
        root.set("furniture.admin-override-enabled", false);
        root.set("furniture.admin-debug-enabled", false);
    }
}
