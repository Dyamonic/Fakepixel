package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;

public final class RegionDefaults {
    private RegionDefaults() {
    }

    public static void apply(ConfigurationSection root) {
        root.set("regions.enabled", true);
        root.set("regions.default-discover-sound", "ENTITY_PLAYER_LEVELUP");
    }
}
