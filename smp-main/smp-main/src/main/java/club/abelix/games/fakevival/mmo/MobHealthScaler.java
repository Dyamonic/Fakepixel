package club.abelix.games.fakevival.mmo;

import org.bukkit.configuration.ConfigurationSection;

public final class MobHealthScaler {
    private final ConfigurationSection settings;

    public MobHealthScaler(ConfigurationSection settings) {
        this.settings = settings;
    }

    public double normalHealth(int level) {
        double base = Math.max(1.0D, settings.getDouble("mobs.normal.baseHealth", 250.0D));
        double perLevel = Math.max(0.0D, settings.getDouble("mobs.normal.healthPerLevel", 50.0D));
        return base + Math.max(0, level) * perLevel;
    }

    public double zoneBossHealth(int level, double configuredHealth) {
        return bossHealth(level, configuredHealth, "bosses.zone", 8.0D, 2500.0D, 5.0D);
    }

    public double dungeonBossHealth(int level, double configuredHealth) {
        return bossHealth(level, configuredHealth, "bosses.dungeon", 12.0D, 10000.0D, 8.0D);
    }

    private double bossHealth(int level, double configuredHealth, String path, double defaultMultiplier,
                              double defaultFlatBonus, double defaultMinimumMultiplier) {
        double normal = normalHealth(level);
        double multiplier = Math.max(0.0D, settings.getDouble(path + ".healthMultiplier", defaultMultiplier));
        double flatBonus = Math.max(0.0D, settings.getDouble(path + ".flatBonus", defaultFlatBonus));
        double minimumMultiplier = Math.max(1.0D,
                settings.getDouble(path + ".minimumOverNormalMultiplier", defaultMinimumMultiplier));
        double scaled = Math.max(normal * multiplier + flatBonus, normal * minimumMultiplier);
        boolean allowLow = settings.getBoolean("bosses.allowLowConfiguredHealth", false);
        if (allowLow && Double.isFinite(configuredHealth) && configuredHealth > 0.0D) return configuredHealth;
        double configured = Double.isFinite(configuredHealth) ? Math.max(0.0D, configuredHealth) : 0.0D;
        return Math.max(configured, scaled);
    }
}
