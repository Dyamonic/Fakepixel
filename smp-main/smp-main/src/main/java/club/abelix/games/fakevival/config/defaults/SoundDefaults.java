package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;

public final class SoundDefaults {
    private SoundDefaults() {
    }

    public static void apply(ConfigurationSection root) {
        root.set("sounds.skill-level-up.enabled", true);
        root.set("sounds.skill-level-up.sound", "ENTITY_PLAYER_LEVELUP");
        root.set("sounds.skill-level-up.volume", 1.0D);
        root.set("sounds.skill-level-up.pitch", 1.2D);
        root.set("sounds.job-level-up.enabled", true);
        root.set("sounds.job-level-up.sound", "ENTITY_PLAYER_LEVELUP");
        root.set("sounds.job-level-up.volume", 1.0D);
        root.set("sounds.job-level-up.pitch", 1.0D);
    }
}
