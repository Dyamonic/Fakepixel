package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;

public final class NpcDefaults {
    private NpcDefaults() {
    }

    public static void apply(ConfigurationSection root) {
        root.set("npcs.enabled", true);
        root.set("npcs.talking.guide.name", "Guide");
        root.set("npcs.talking.guide.messages", List.of("&bGuide &8> &fWelcome to Fakepixel SMP, &a%player%&f!", "&7Your &6SMP Menu &7is locked in hotbar slot &f9&7. Right-click it to open everything.", "&7Start with &e/skills&7, &e/jobs&7, &e/collections&7, and &e/quests&7.", "&7Use &e/g help &7for guild commands and &e/playtime &7for monthly rewards."));
        root.set("npcs.talking.guide.commands", List.of("player:menu"));
        root.set("npcs.talking.guide.sound", "ENTITY_VILLAGER_YES");
        root.set("npcs.talking.skills.name", "Skills Master");
        root.set("npcs.talking.skills.messages", List.of("&dSkills Master &8> &fBreak blocks, kill mobs, and fish to level skills."));
        root.set("npcs.talking.skills.commands", List.of("player:skills"));
        root.set("npcs.talking.skills.sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
    }
}
