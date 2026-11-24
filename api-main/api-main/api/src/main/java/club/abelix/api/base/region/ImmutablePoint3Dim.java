package club.abelix.api.base.region;

public class ImmutablePoint3Dim extends Point3Dim {

    public ImmutablePoint3Dim(int x, int y, int z) {
        super(x, y, z);
    }

    public ImmutablePoint3Dim() {
        super();
    }

    private ImmutablePoint3Dim throwException() {
        throw new IllegalStateException("Point is immutable");
    }

    protected Point3Dim createPoint(int x, int y, int z) {
        return new ImmutablePoint3Dim(x, y, z);
    }

    @Override
    public void setX(int x) {
        throwException();
    }

    @Override
    public void setZ(int z) {
        throwException();
    }

    @Override
    public void setY(int y) {
        throwException();
    }

    @Override
    public ImmutablePoint3Dim add(int x, int y, int z) {
        return throwException();
    }

    @Override
    public ImmutablePoint3Dim add(int x, int z) {
        return throwException();
    }

    @Override
    public ImmutablePoint3Dim subtract(int x, int y, int z) {
        return throwException();
    }

    @Override
    public ImmutablePoint3Dim subtract(int x, int z) {
        return throwException();
    }

    @Override
    public ImmutablePoint3Dim immutable() {
        return this;
    }

    @Override
    public Point3Dim mutable() {
        return new Point3Dim(x, y, z);
    }

    @Override
    public boolean isImmutable() {
        return true;
    }

}
