package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;

public final class SpawnDefaults {
    private SpawnDefaults() {
    }

    public static void apply(ConfigurationSection root) {
        root.set("spawn.teleport-on-join", true);
    }
}
