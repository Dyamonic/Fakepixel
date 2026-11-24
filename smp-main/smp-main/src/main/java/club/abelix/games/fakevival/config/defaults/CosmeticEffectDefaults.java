package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

public final class CosmeticEffectDefaults {
    private CosmeticEffectDefaults() {
    }

    public static void apply(ConfigurationSection root) {
        root.set("cosmetic-effects.enabled", true);
        root.set("cosmetic-effects.update-interval-ticks", 10);
        root.set("cosmetic-effects.sword-slashes.enabled", false);
        root.set("weaponSlashParticles", false);
    }
}
