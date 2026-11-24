package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

public final class CustomEnchantDefaults {
    private CustomEnchantDefaults() {
    }

    public static void apply(ConfigurationSection root) {
        root.set("custom-enchants.enchant-table.enabled", true);
        root.set("custom-enchants.enchant-table.chance", 35.0D);
    }
}
