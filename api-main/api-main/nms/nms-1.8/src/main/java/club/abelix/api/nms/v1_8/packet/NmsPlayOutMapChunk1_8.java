package club.abelix.api.nms.v1_8.packet;

import club.abelix.api.nms.packet.NmsPlayOutMapChunk;
import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunk;

public class NmsPlayOutMapChunk1_8 extends NmsPlayOutMapChunk {

    private final PacketPlayOutMapChunk packet;

    public NmsPlayOutMapChunk1_8(PacketPlayOutMapChunk packet) {
        this.packet = packet;
    }

    public PacketPlayOutMapChunk.ChunkMap getChunkMap() {
        return getField(PacketPlayOutMapChunk.ChunkMap.class, "c", packet);
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
        return getChunkMap().b;
    }

    @Override
    public boolean isFull() {
        return getBooleanField("d", packet);
    }

}
