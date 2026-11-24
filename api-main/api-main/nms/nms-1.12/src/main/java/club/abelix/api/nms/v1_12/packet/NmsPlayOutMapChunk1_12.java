package club.abelix.api.nms.v1_12.packet;

import club.abelix.api.nms.packet.NmsPlayOutMapChunk;
import net.minecraft.server.v1_12_R1.PacketPlayOutMapChunk;

public class NmsPlayOutMapChunk1_12 extends NmsPlayOutMapChunk {

    private final PacketPlayOutMapChunk packet;

    public NmsPlayOutMapChunk1_12(PacketPlayOutMapChunk packet) {
        this.packet = packet;
    }

    public PacketPlayOutMapChunk getPacket() {
        return packet;
    }

    @Override
    public int getX() {
        return getIntField("a", packet);
    }

    @Override
    public int getZ() {
        return getIntField("b", packet);
    }

    @Override
    public int getMask() {
        return getIntField("c", packet);
    }

    @Override
    public boolean isFull() {
        return getBooleanField("f", packet);
    }

}
