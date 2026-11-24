package club.abelix.games.fakevival.mobengine.mob;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class SMPMobActivationRange {
    public static final double FULL_COMBAT_RANGE_SQUARED = SMPMobEngineSettings.FULL_VISIBILITY_RANGE * SMPMobEngineSettings.FULL_VISIBILITY_RANGE;
    public static final double MEDIUM_AI_RANGE_SQUARED = SMPMobEngineSettings.REDUCED_VISIBILITY_RANGE * SMPMobEngineSettings.REDUCED_VISIBILITY_RANGE;
    public static final double SLOW_AI_RANGE_SQUARED = SMPMobEngineSettings.HIDDEN_VISIBILITY_RANGE * SMPMobEngineSettings.HIDDEN_VISIBILITY_RANGE;

    public Activation nearestActivation(SMPMob mob) {
        Player nearest = null;
        double best = Double.MAX_VALUE;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isValid() || !player.getWorld().equals(mob.getEntity().getWorld())) {
                continue;
            }
            double distanceSquared = player.getLocation().distanceSquared(mob.getLocation());
            if (distanceSquared < best) {
                best = distanceSquared;
                nearest = player;
            }
        }

        if (nearest == null) {
            return new Activation(null, Double.MAX_VALUE, SMPMob.State.SLEEPING);
        }
        if (best <= FULL_COMBAT_RANGE_SQUARED) {
            return new Activation(nearest, best, SMPMob.State.FULL_COMBAT);
        }
        if (best <= MEDIUM_AI_RANGE_SQUARED) {
            return new Activation(nearest, best, SMPMob.State.MEDIUM_AI);
        }
        if (best <= SLOW_AI_RANGE_SQUARED) {
            return new Activation(nearest, best, SMPMob.State.SLOW_AI);
        }
        return new Activation(nearest, best, SMPMob.State.VIRTUALIZED);
    }

    public record Activation(Player player, double distanceSquared, SMPMob.State state) {
    }
}
