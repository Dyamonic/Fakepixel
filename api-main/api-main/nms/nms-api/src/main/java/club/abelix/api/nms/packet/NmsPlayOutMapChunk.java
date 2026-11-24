package club.abelix.api.nms.packet;

public abstract class NmsPlayOutMapChunk extends NmsPacket {

    public abstract int getX();

    public abstract int getZ();

    public abstract int getMask();

    public abstract boolean isFull();


}
