package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;

public final class MainMenuDefaults {
    private MainMenuDefaults() {
    }

    public static void apply(ConfigurationSection root) {
        root.set("main-menu.title", "&8Survival Menu");
        root.set("main-menu.item.material", "NETHER_STAR");
        root.set("main-menu.item.name", "&aSurvival Menu");
        root.set("main-menu.item.lore", List.of(
                "&7View your profile, skills,",
                "&7collections, quests, dungeons,",
                "&7storage, bank, and more.",
                "",
                "&eRight Click to open!"));
    }
}
