package club.abelix.api.base.region;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: agera-api</p>
 * <p>CuboidRegion creation on 26.04.2024 at 23:20</p>
 */


@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public final class CuboidRegion extends Region3Dim {

    private int minX;
    private int maxX;
    private int minY;
    private int maxY;
    private int minZ;
    private int maxZ;

    public CuboidRegion(Location location) {
        this(location, 1);
    }

    public CuboidRegion(Location location, int radius) {
        this(location.clone().subtract(radius, radius, radius),
                location.clone().add(radius, radius, radius));
    }

    public CuboidRegion(Location loc1, Location loc2) {
        this(new Point3Dim(loc1.getBlockX(), loc1.getBlockY(), loc1.getBlockZ()),
                new Point3Dim(loc2.getBlockX(), loc2.getBlockY(), loc2.getBlockZ()));
    }

    public CuboidRegion(Point3Dim first, Point3Dim second) {
        super(first, second);

        minX = Math.min(first.getX(), second.getX());
        maxX = Math.max(first.getX(), second.getX());

        minY = Math.min(first.getY(), second.getY());
        maxY = Math.max(first.getY(), second.getY());

        minZ = Math.min(first.getZ(), second.getZ());
        maxZ = Math.max(first.getZ(), second.getZ());
    }

    public boolean isInRegion(Location l) {
        return (l.getBlockX() <= maxX && l.getBlockX() >= minX)
                && (l.getY() <= maxY && l.getY() >= minY)
                && (l.getBlockZ() <= maxZ && l.getBlockZ() >= minZ);
    }
}
