package club.abelix.api.base.box.animation.protection;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Честно понятия не имею как это работает, все вопросы к <a href="https://chatgpt.com/?model=gpt-5">...</a>
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BoxOpenProtectionSphere extends BukkitRunnable {

    final World world;
    final Location center;
    final double radius;
    final UUID allowedId;
    final Location tether;
    final Effect effect;
    final double knockback;
    final double epsilon;
    final List<Vector> hemiPoints;
    final int density;
    boolean allowedPrevInside = false;
    int allowedTpCooldown = 0; // тики, анти-дрожание

    public BoxOpenProtectionSphere(@NonNull Location center,
                                   double radius,
                                   @NonNull Player allowed,
                                   @NonNull Location tether,
                                   Effect effect,
                                   double knockback,
                                   int density,
                                   double epsilon) {
        Objects.requireNonNull(center.getWorld());

        this.world = center.getWorld();
        this.center = center.clone();
        this.radius = radius;
        this.allowedId = allowed.getUniqueId();
        this.tether = tether.clone();
        this.effect = effect != null ? effect : Effect.FIREWORKS_SPARK;
        this.knockback = knockback > 0 ? knockback : 0.3;
        this.density = Math.max(1, density);
        this.epsilon = epsilon > 0 ? epsilon : 0.2;
        this.hemiPoints = precomputeHemispherePoints();
    }

    private List<Vector> precomputeHemispherePoints() {
        List<Vector> pts = new ArrayList<>();
        int rings = Math.max(density, (int) Math.round(radius * density));
        int slices = rings * 2;
        for (int i = 0; i <= rings; i++) {
            double theta = (Math.PI / 2.0) * i / rings;
            double sinT = Math.sin(theta);
            double cosT = Math.cos(theta);
            for (int j = 0; j <= slices; j++) {
                double phi = (2.0 * Math.PI) * j / slices;
                double sinP = Math.sin(phi);
                double cosP = Math.cos(phi);
                double x = radius * sinT * cosP;
                double y = radius * cosT;
                double z = radius * sinT * sinP;
                pts.add(new Vector(x, y, z));
            }
        }
        return pts;
    }

    @Override
    public void run() {
        if (!isWorldValid()) {
            cancel();
            return;
        }

        if (allowedTpCooldown > 0) allowedTpCooldown--;

        renderHemisphereParticles();
        applyBarrierPhysics();
    }

    private boolean isWorldValid() {
        return world != null && Bukkit.getWorld(world.getUID()) != null;
    }

    private void renderHemisphereParticles() {
        double cx = center.getX();
        double cy = center.getY();
        double cz = center.getZ();

        for (int i = 0; i < hemiPoints.size(); i += 2) {
            val off = hemiPoints.get(i);
            Location loc = new Location(world, cx + off.getX(), cy + off.getY(), cz + off.getZ());
            world.spigot().playEffect(loc, effect, 0, 0, 0, 0, 0, 0, 2, 15);
        }
    }

    private void applyBarrierPhysics() {
        final double r2 = radius * radius;
        final double cx = center.getX();
        final double cy = center.getY();
        final double cz = center.getZ();

        final double range2 = (radius + 2.0) * (radius + 2.0);
        for (Player p : world.getPlayers()) {
            if (p.getUniqueId().equals(allowedId)) continue;
            Vector v = p.getLocation().toVector().subtract(new Vector(cx, cy, cz));
            double d2 = v.lengthSquared();
            if (d2 > range2) continue;
            if (d2 < r2) {
                Vector push = v.normalize().multiply(knockback);
                if (push.getY() < 0.3) push.setY(0.3);
                p.setVelocity(push);
            }
        }

        Player allowed = Bukkit.getPlayer(allowedId);
        if (allowed == null || !allowed.isOnline() || allowed.getWorld() != world) {
            allowedPrevInside = false;
            return;
        }

        Vector av = allowed.getLocation().toVector().subtract(new Vector(cx, cy, cz));
        double ad2 = av.lengthSquared();
        boolean insideNow = ad2 < r2;

        if (allowedTpCooldown == 0 && allowedPrevInside && !insideNow) {
            Location target = safeTetherInside();
            allowed.teleport(target);
            allowedTpCooldown = 2;
            insideNow = true;
        }

        allowedPrevInside = insideNow;
    }

    private Location safeTetherInside() {
        Location t = tether;
        if (t.getWorld() != world) return center.clone();

        double r2 = radius * radius;
        if (t.distanceSquared(center) < r2 - 1e-6) return t;

        Vector dir = t.toVector().subtract(center.toVector());
        if (dir.lengthSquared() < 1e-8) dir = new Vector(0, 1, 0);
        dir.normalize().multiply(Math.max(0.0, radius - Math.max(0.25, epsilon) - 0.5));
        return center.clone().add(dir);
    }

    public static BoxOpenProtectionSphere simple(Location center, double radius, Player allowed, Location tether) {
        return new BoxOpenProtectionSphere(center, radius, allowed, tether, Effect.MAGIC_CRIT, 0.9, 2, 0.6);
    }

}
