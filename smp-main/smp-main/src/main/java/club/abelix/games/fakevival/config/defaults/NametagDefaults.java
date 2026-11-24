package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;

public final class NametagDefaults {
    private NametagDefaults() {
    }

    public static void apply(ConfigurationSection root) {
        root.set("nametag.enabled", true);
        root.set("nametag.format", "%player_level_display%%abelix_prefix%%player%");
    }
}
