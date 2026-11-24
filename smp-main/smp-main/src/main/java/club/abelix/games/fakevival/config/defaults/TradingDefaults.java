package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;

public final class TradingDefaults {
    private TradingDefaults() {
    }

    public static void apply(ConfigurationSection root) {
        root.set("trading.request-cooldown-seconds", 5);
    }
}
