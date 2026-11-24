package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

public final class EventDefaults {
    private EventDefaults() {
    }

    public static void apply(ConfigurationSection root) {
        root.set("events.enabled", true);
        root.set("events.min-downtime-minutes", 30L);
        root.set("events.max-downtime-minutes", 60L);
        root.set("events.minecraft-day-real-minutes", 20.0D);
    }
}
