package club.abelix.api.base.region;

import java.util.Iterator;

public class Region3Dim extends Region<Point3Dim> {

    public Region3Dim(Point3Dim first, Point3Dim second) {
        super(first, second);
    }

    public Region3Dim(int x1, int y1, int z1, int x2, int y2, int z2) {
        this(new Point3Dim(x1, y1, z1), new Point3Dim(x2, y2, z2));
    }

    @Override
    public Region3Dim asBlockRegion() {
        return new Region3Dim(
                minimumPoint.getX() << 4,
                minimumPoint.getY() << 4,
                minimumPoint.getZ() << 4,
                maximumPoint.getX() << 4,
                maximumPoint.getY() << 4,
                maximumPoint.getZ() << 4
        );
    }

    @Override
    public boolean contains(int x, int y, int z) {
        return y >= minimumPoint.y && y <= maximumPoint.y && contains(x, z);
    }

    @Override
    public boolean contains(int x, int z) {
        return x >= minimumPoint.x && z >= minimumPoint.z && x <= maximumPoint.x && z <= maximumPoint.z;
    }

    @Override
    public Iterator<Point3Dim> iterator() {
        return new Region3DimIterator();
    }

    private class Region3DimIterator implements Iterator<Point3Dim> {
        private int x = minimumPoint.getX(), y = minimumPoint.getY(), z = minimumPoint.getZ();

        @Override
        public boolean hasNext() {
            return z <= maximumPoint.getZ();
        }

        @Override
        public Point3Dim next() {
            Point3Dim point = new Point3Dim(x, y, z);

            if (x++ == maximumPoint.getX()) {
                x = minimumPoint.getX();

                if (y++ == maximumPoint.getY()) {
                    y = minimumPoint.getY();
                    z++;
                }
            }

            return point;
        }
    }
}
