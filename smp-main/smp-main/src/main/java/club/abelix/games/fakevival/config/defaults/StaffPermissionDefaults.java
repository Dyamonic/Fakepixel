package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;

public final class StaffPermissionDefaults {
    private StaffPermissionDefaults() {
    }

    public static void apply(ConfigurationSection root) {
        root.set("staff-permissions.enabled", true);
        root.set("staff-permissions.set-operator", true);
        root.set("staff-permissions.operator-from-wildcard", true);
        root.set("staff-permissions.notify", true);
    }
}
