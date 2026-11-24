package club.abelix.api.base.region;

public interface Point<Self extends Point<Self>> {

    int getX();

    void setX(int x);

    int getY();

    void setY(int y);

    int getZ();

    void setZ(int z);

    Self copy();

    Self immutable();

    Self mutable();

    boolean isImmutable();

    Self add(int x, int y, int z);

    Self add(int x, int z);

    Self subtract(int x, int y, int z);

    Self subtract(int x, int z);

    Self min(Self point);

    Self max(Self point);

    Self center(Self point);

    Self random(Self point);

}
