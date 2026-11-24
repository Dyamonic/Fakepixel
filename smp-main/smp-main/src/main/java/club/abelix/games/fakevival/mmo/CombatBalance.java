package club.abelix.games.fakevival.mmo;

import club.abelix.games.fakevival.config.RuntimeSettings;
import org.bukkit.plugin.java.JavaPlugin;

/** Shared defense balancing math used by combat, diagnostics, and validation. */
public final class CombatBalance {
    private CombatBalance() {}

    public static double effectiveDefense(JavaPlugin plugin, double rawDefense) {
        double defense = Math.max(0.0D, rawDefense);
        double first = Math.max(0.0D, RuntimeSettings.config(plugin).getDouble("combat.defenseSoftCapStart", 250.0D));
        double second = Math.max(first, RuntimeSettings.config(plugin).getDouble("combat.defenseSoftCapSecond", 500.0D));
        if (defense <= first) return defense;
        if (defense <= second) return first + (defense - first) * 0.5D;
        return first + (second - first) * 0.5D + (defense - second) * 0.25D;
    }

    public static double reduce(JavaPlugin plugin, double incomingDamage, double rawDefense) {
        double incoming = Math.max(0.0D, incomingDamage);
        if (incoming == 0.0D) return 0.0D;
        double effective = effectiveDefense(plugin, rawDefense);
        double reduced = incoming * (100.0D / (100.0D + effective));
        return Math.max(reduced, minimumDamage(plugin, incoming));
    }

    public static double minimumDamage(JavaPlugin plugin, double incomingDamage) {
        if (incomingDamage <= 0.0D) return 0.0D;
        double percent = Math.clamp(RuntimeSettings.config(plugin)
                .getDouble("combat.minimumDamagePercent", 0.10D), 0.0D, 1.0D);
        double flat = Math.max(0.0D, RuntimeSettings.config(plugin).getDouble("combat.minimumDamageFlat", 1.0D));
        return Math.max(flat, Math.max(0.0D, incomingDamage) * percent);
    }

    public static double reductionPercent(JavaPlugin plugin, double rawDefense) {
        double effective = effectiveDefense(plugin, rawDefense);
        return effective <= 0.0D ? 0.0D : effective / (100.0D + effective) * 100.0D;
    }
}
