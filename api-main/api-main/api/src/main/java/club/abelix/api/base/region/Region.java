package club.abelix.api.base.region;

import lombok.Data;

// todo документация
@Data
public abstract class Region<P extends Point<P>> implements Iterable<P> {

    protected P minimumPoint, maximumPoint;

    public Region(P first, P second) {
        setPoints(first, second);
    }

    /**
     * Средняя точка в регионе
     */
    public P getCenterPoint() {
        return minimumPoint.center(maximumPoint);
    }

    /**
     * Рандомная точка в регионе
     */
    public P getRandomPoint() {
        return minimumPoint.random(maximumPoint);
    }

    public void setSecondPoint(P point) {
        setPoints(minimumPoint, point);
    }

    public void setFirstPoint(P point) {
        setPoints(maximumPoint, point);
    }

    /**
     * Установить первую и вторую точку
     */
    public void setPoints(P first, P second) {
        minimumPoint = first.min(second);
        maximumPoint = first.max(second);
    }

    /**
     * Проверить, есть ли в регионе точка с координатой X, Y, Z
     */
    public abstract boolean contains(int x, int y, int z);

    /**
     * Проверить, есть ли в регионе точка с координатой X, Z
     */
    public abstract boolean contains(int x, int z);

    public Region2Dim asChunkRegion() {
        return new Region2Dim(
                getMinimumPoint().getX() >> 4,
                getMinimumPoint().getZ() >> 4,
                getMaximumPoint().getX() >> 4,
                getMaximumPoint().getZ() >> 4
        );
    }

    public abstract Region<P> asBlockRegion();

}
