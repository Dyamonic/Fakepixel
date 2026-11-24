package club.abelix.api.base.region;

import java.util.Iterator;

public class Region2Dim extends Region<Point2Dim> {

    public Region2Dim(Point2Dim first, Point2Dim second) {
        super(first, second);
    }

    public Region2Dim(int x1, int z1, int x2, int z2) {
        this(new Point2Dim(x1, z1), new Point2Dim(x2, z2));
    }

    @Override
    public boolean contains(int x, int y, int z) {
        return contains(x, z);
    }

    @Override
    public boolean contains(int x, int z) {
        return x >= minimumPoint.x && z >= minimumPoint.z && x <= maximumPoint.x && z <= maximumPoint.z;
    }

    @Override
    public Region2Dim asBlockRegion() {
        return new Region2Dim(
                minimumPoint.getX() << 4,
                minimumPoint.getZ() << 4,
                maximumPoint.getX() << 4,
                maximumPoint.getZ() << 4
        );
    }

    @Override
    public Iterator<Point2Dim> iterator() {
        return new Region2DimIterator();
    }

    private class Region2DimIterator implements Iterator<Point2Dim> {
        private int x = minimumPoint.getX(), z = minimumPoint.getZ();

        @Override
        public boolean hasNext() {
            return z <= maximumPoint.getZ();
        }

        @Override
        public Point2Dim next() {
            Point2Dim point = new Point2Dim(x, z);

            if (x++ == maximumPoint.getX()) {
                x = minimumPoint.getX();
                z++;
            }
            return point;
        }
    }
}
