package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;

public final class ScoreboardDefaults {
    private ScoreboardDefaults() {
    }

    public static void apply(ConfigurationSection root) {
        root.set("scoreboard.enabled", true);
        root.set("scoreboard.update-interval-ticks", 40);
        root.set("scoreboard.title", "&6&lSMP");
        root.set("scoreboard.lines", List.of("&7%date%", "&7", "&r%abelix_prefix%%player%", "&fKills: &e%kills%", "&fDeath: &c%deaths%", "&7", "&fCoins: &e%coins%", "&fGold: &6%gold%", "&7", "&emc.fakepixel.me"));
    }
}
