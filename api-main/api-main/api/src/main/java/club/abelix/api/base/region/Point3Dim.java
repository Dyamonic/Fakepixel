package club.abelix.api.base.region;

import com.sk89q.worldedit.Vector;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.concurrent.ThreadLocalRandom;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Point3Dim implements Point<Point3Dim> {

    protected int x, y, z;

    protected Point3Dim createPoint(int x, int y, int z) {
        return new Point3Dim(x, y, z);
    }

    @Override
    public Point3Dim copy() {
        return createPoint(x, y, z);
    }

    @Override
    public ImmutablePoint3Dim immutable() {
        return new ImmutablePoint3Dim(x, y, z);
    }

    @Override
    public Point3Dim mutable() {
        return this;
    }

    @Override
    public boolean isImmutable() {
        return false;
    }

    @Override
    public Point3Dim add(int x, int y, int z) {
        this.x += x;
        this.y += y;
        this.z += z;

        return this;
    }

    @Override
    public Point3Dim add(int x, int z) {
        return add(x, 0, z);
    }

    @Override
    public Point3Dim subtract(int x, int y, int z) {
        this.x -= x;
        this.y -= y;
        this.z -= z;

        return this;
    }

    @Override
    public Point3Dim subtract(int x, int z) {
        return subtract(x, 0, z);
    }

    @Override
    public Point3Dim min(Point3Dim point) {
        return createPoint(Math.min(x, point.x), Math.min(y, point.y), Math.min(z, point.z));
    }

    @Override
    public Point3Dim max(Point3Dim point) {
        return createPoint(Math.max(x, point.x), Math.max(y, point.y), Math.max(z, point.z));
    }

    public Vector asVector() {
        return new Vector(x, y, z);
    }

    @Override
    public Point3Dim center(Point3Dim point) {
        return createPoint(
                (int) (x + point.x / 2D),
                (int) (y + point.y / 2D),
                (int) (z + point.z / 2D)
        );
    }

    @Override
    public Point3Dim random(Point3Dim point) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        return createPoint(
                random.nextInt(Math.min(x, point.x), Math.max(x, point.x)),
                random.nextInt(Math.min(y, point.y), Math.max(y, point.y)),
                random.nextInt(Math.min(z, point.z), Math.max(z, point.z))
        );
    }

    public Location toLocation(World world) {
        return new Location(world, x, y, z);
    }

}
