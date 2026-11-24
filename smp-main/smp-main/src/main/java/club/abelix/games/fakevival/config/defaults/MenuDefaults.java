package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;

public final class MenuDefaults {
    private MenuDefaults() {
    }

    public static void apply(ConfigurationSection root) {
        root.set("menus.main.title", "&8Survival Menu");
        root.set("menus.sellall.title", "&8Sell All");
        root.set("menus.skills.title", "&8Your Skills");
        root.set("menus.jobs.title", "&8Jobs");
        root.set("menus.skill-leveling.title", "&8Skill Leveling");
        root.set("menus.mining-tree.title", "&8Mining Skill Tree");
        root.set("menus.skill-tree.title", "&8%skill% Skill Tree");
        root.set("menus.adventure-guild.title", "&5Adventure Guild");
        root.set("menus.collections.title", "&8Collections");
    }
}
