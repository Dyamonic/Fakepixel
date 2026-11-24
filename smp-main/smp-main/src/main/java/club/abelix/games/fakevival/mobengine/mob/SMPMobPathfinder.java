package club.abelix.games.fakevival.mobengine.mob;

import io.papermc.paper.entity.LookAnchor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class SMPMobPathfinder {
    private static final long PATH_INTERVAL_TICKS = 10L;
    private static final double STOP_RANGE_SQUARED = 2.4 * 2.4;
    private static final double MOVE_SPEED = 0.24;

    public boolean moveToward(SMPMob mob, Player target, long tick) {
        if (tick - mob.getLastPathfindTick() < PATH_INTERVAL_TICKS) {
            return false;
        }

        Location from = mob.getLocation();
        Location to = target.getLocation();
        if (!from.getWorld().equals(to.getWorld()) || from.distanceSquared(to) <= STOP_RANGE_SQUARED) {
            mob.setLastPathfindTick(tick);
            return true;
        }

        Vector direction = to.toVector().subtract(from.toVector());
        direction.setY(0.0);
        if (direction.lengthSquared() < 0.001) {
            mob.setLastPathfindTick(tick);
            return true;
        }

        Vector velocity = direction.normalize().multiply(MOVE_SPEED);
        velocity.setY(mob.getEntity().getVelocity().getY());
        mob.getEntity().setVelocity(velocity);
        Location eyes = target.getEyeLocation();
        mob.getEntity().lookAt(eyes.getX(), eyes.getY(), eyes.getZ(), LookAnchor.EYES);
        mob.setLastPathfindTick(tick);
        return true;
    }
}
