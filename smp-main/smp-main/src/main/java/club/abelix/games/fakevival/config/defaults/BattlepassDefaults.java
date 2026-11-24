package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

public final class BattlepassDefaults {
    private BattlepassDefaults() {
    }

    public static void apply(ConfigurationSection root) {
        root.set("battlepass.enabled", true);
        root.set("battlepass.play-xp-every-minutes", 5);
        root.set("battlepass.play-xp", 25);
    }
}
