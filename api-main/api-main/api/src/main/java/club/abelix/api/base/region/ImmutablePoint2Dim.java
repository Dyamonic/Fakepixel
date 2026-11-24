package club.abelix.api.base.region;

public class ImmutablePoint2Dim extends Point2Dim {

    public ImmutablePoint2Dim(int x, int z) {
        super(x, z);
    }

    public ImmutablePoint2Dim() {
        super();
    }

    private ImmutablePoint2Dim throwException() {
        throw new IllegalStateException("Point is immutable");
    }

    @Override
    protected Point2Dim createPoint(int x, int z) {
        return new ImmutablePoint2Dim(x, z);
    }

    @Override
    public void setX(int x) {
        throwException();
    }

    @Override
    public void setY(int y) {
        throwException();
    }

    @Override
    public void setZ(int z) {
        throwException();
    }

    @Override
    public ImmutablePoint2Dim add(int x, int y, int z) {
        return throwException();
    }

    @Override
    public ImmutablePoint2Dim add(int x, int z) {
        return throwException();
    }

    @Override
    public ImmutablePoint2Dim subtract(int x, int y, int z) {
        return throwException();
    }

    @Override
    public ImmutablePoint2Dim subtract(int x, int z) {
        return throwException();
    }

    @Override
    public ImmutablePoint2Dim immutable() {
        return this;
    }

    @Override
    public Point2Dim mutable() {
        return new Point2Dim(x, z);
    }

    @Override
    public boolean isImmutable() {
        return true;
    }
}
