package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;

public final class EntityDeathMessageDefaults {
    private EntityDeathMessageDefaults() {
    }

    public static void apply(ConfigurationSection root) {
        root.set("entity-death-messages.hide-named-entity-deaths", true);
    }
}
