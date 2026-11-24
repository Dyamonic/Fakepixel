package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;

public final class TablistDefaults {
    private TablistDefaults() {
    }

    public static void apply(ConfigurationSection root) {
        root.set("tablist.enabled", true);
        root.set("tablist.update-ticks", 40);
        root.set("tablist.name-format", "%player_level_display%%abelix_prefix%%player%");
        root.set("tablist.header", List.of("&bYou are playing on &eMC.FAKEPIXEL.ME"));
        root.set("tablist.footer", List.of("&aRanks, Boosters & MORE! &cSTORE.FAKEPIXEL.ME"));
    }
}
