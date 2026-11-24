package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;

public final class PveDefaults {
    private PveDefaults() {
    }

    public static void apply(ConfigurationSection root) {
        root.set("pve.default-enabled", false);
        root.set("pve.combat-tag-seconds", 5);
        root.set("pve.disable-pvp-in-spawn", true);
        root.set("pve.use-enabled-worlds", true);
        root.set("pve.enabled-worlds", List.of("world"));
    }
}
