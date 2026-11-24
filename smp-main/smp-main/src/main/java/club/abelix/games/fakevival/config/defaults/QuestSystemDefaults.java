package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;

public final class QuestSystemDefaults {
    private QuestSystemDefaults() {
    }

    public static void apply(ConfigurationSection root) {
        root.set("quests.enabled", false);
    }
}
