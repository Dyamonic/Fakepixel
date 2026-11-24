package club.abelix.api.nms.v1_16.packet;

import club.abelix.api.nms.packet.NmsPlayOutMapChunk;
import net.minecraft.server.v1_16_R3.PacketPlayOutMapChunk;

public class NmsPlayOutMapChunk1_16 extends NmsPlayOutMapChunk {

    private final PacketPlayOutMapChunk packet;

    public NmsPlayOutMapChunk1_16(PacketPlayOutMapChunk packet) {
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
        return getBooleanField("h", packet);
    }

}
