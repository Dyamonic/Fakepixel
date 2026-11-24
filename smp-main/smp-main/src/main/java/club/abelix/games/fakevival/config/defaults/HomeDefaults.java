package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;

public final class HomeDefaults {
    private HomeDefaults() {
    }

    public static void apply(ConfigurationSection root) {
        root.set("homes.max-homes", 5);
    }
}
