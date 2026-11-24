package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;

public final class TpaDefaults {
    private TpaDefaults() {
    }

    public static void apply(ConfigurationSection root) {
        root.set("tpa.request-timeout-seconds", 15);
    }
}
