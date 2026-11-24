package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

public final class DungeonRuntimeDefaults {
    private DungeonRuntimeDefaults() {}
    public static void apply(ConfigurationSection root) {
        root.set("dungeons.empty-timeout-seconds", 300);
        root.set("dungeons.participant-location-save-seconds", 10);
    }
}
