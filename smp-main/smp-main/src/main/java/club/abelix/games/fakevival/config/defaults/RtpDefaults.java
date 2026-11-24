package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;

public final class RtpDefaults {
    private RtpDefaults() {
    }

    public static void apply(ConfigurationSection root) {
        root.set("rtp.world", "world");
        root.set("rtp.spawn-world", "hub");
        root.set("rtp.min-radius", 500);
        root.set("rtp.max-radius", 5000);
        root.set("rtp.attempts", 80);
        root.set("rtp.cooldown-seconds", 60);
    }
}
