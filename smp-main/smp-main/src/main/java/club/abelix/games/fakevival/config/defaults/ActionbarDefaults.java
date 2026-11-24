package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

public final class ActionbarDefaults {
    private ActionbarDefaults() {
    }

    public static void apply(ConfigurationSection root) {
        root.set("actionbar.enabled", true);
        root.set("actionbar.update-interval-ticks", 10);
        root.set("actionbar.format", "&c%current_mmo_hp%/%max_mmo_hp%❤  &a%defense%/%max_defense%❈ Defense  &b%current_mana%/%max_mana%✎ Mana");
        root.set("actionbar.mana.max", 100);
        root.set("actionbar.mana.regen-per-update", 1);
    }
}
