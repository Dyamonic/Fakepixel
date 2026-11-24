package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

public final class BossbarDefaults {
    private BossbarDefaults() {
    }

    public static void apply(ConfigurationSection root) {
        root.set("bossbar.hide-after-ticks", 60);
        root.set("bossbar.skills.enabled", true);
        root.set("bossbar.skills.color", "BLUE");
        root.set("bossbar.skills.title", "&b%skill% &7Level &a%level% &8- &e%xp%&7/&e%needed% XP");
        root.set("bossbar.jobs.enabled", true);
        root.set("bossbar.jobs.color", "YELLOW");
        root.set("bossbar.jobs.title", "&6%job% Job &7Level &a%level% &8- &e%xp%&7/&e%needed% XP");
    }
}
