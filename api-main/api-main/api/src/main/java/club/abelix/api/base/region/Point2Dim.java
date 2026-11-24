package club.abelix.api.base.region;

import com.sk89q.worldedit.Vector;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.concurrent.ThreadLocalRandom;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Point2Dim implements Point<Point2Dim> {

    protected int x, z;

    public int getY() {
        throw new UnsupportedOperationException("Y");
    }

    public void setY(int y) {
        throw new UnsupportedOperationException("Y");
    }

    protected Point2Dim createPoint(int x, int z) {
        return new Point2Dim(x, z);
    }

    @Override
    public Point2Dim copy() {
        return createPoint(x, z);
    }

    @Override
    public ImmutablePoint2Dim immutable() {
        return new ImmutablePoint2Dim(x, z);
    }

    @Override
    public Point2Dim mutable() {
        return this;
    }

    @Override
    public boolean isImmutable() {
        return false;
    }

    @Override
    public Point2Dim add(int x, int y, int z) {
        return add(x, z);
    }

    @Override
    public Point2Dim add(int x, int z) {
        this.x += x;
        this.z += z;

        return this;
    }

    @Override
    public Point2Dim subtract(int x, int y, int z) {
        return subtract(x, z);
    }

    @Override
    public Point2Dim subtract(int x, int z) {
        this.x -= x;
        this.z -= z;

        return this;
    }

    @Override
    public Point2Dim min(Point2Dim point) {
        return createPoint(Math.min(x, point.x), Math.min(z, point.z));
    }

    @Override
    public Point2Dim max(Point2Dim point) {
        return createPoint(Math.max(x, point.x), Math.max(z, point.z));
    }

    @Override
    public Point2Dim center(Point2Dim point) {
        return createPoint((int) Math.ceil((x + point.x) / 2D), (int) Math.ceil((z + point.z) / 2D));
    }

    @Override
    public Point2Dim random(Point2Dim point) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        return createPoint(
                random.nextInt(Math.min(x, point.x), Math.max(x, point.x)),
                random.nextInt(Math.min(z, point.z), Math.max(z, point.z))
        );
    }

    public Vector asVector(int y) {
        return new Vector(x, y, z);
    }

}
