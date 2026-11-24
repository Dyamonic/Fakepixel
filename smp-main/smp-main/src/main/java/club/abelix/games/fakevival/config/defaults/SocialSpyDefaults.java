package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;

public final class SocialSpyDefaults {
    private SocialSpyDefaults() {
    }

    public static void apply(ConfigurationSection root) {
        root.set("socialspy.auto-enabled", false);
    }
}
