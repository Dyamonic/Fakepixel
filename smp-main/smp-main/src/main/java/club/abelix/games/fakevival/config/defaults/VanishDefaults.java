package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;

public final class VanishDefaults {
    private VanishDefaults() {
    }

    public static void apply(ConfigurationSection root) {
        root.set("vanish.double-shift-millis", 500);
    }
}
