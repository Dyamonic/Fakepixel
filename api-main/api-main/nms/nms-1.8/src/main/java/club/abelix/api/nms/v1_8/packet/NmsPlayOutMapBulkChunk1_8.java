package club.abelix.api.nms.v1_8.packet;

import club.abelix.api.nms.packet.NmsPlayOutMapChunkBulk;
import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunk;
import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunkBulk;

import java.util.Arrays;

public class NmsPlayOutMapBulkChunk1_8 extends NmsPlayOutMapChunkBulk {

    private final PacketPlayOutMapChunkBulk packet;

    public NmsPlayOutMapBulkChunk1_8(PacketPlayOutMapChunkBulk packet) {
        this.packet = packet;
    }

    public PacketPlayOutMapChunk.ChunkMap[] getChunkMap() {
        return getField(PacketPlayOutMapChunk.ChunkMap[].class, "c", packet);
    }

    @Override
    public int[] getX() {
        return getField(int[].class, "a", packet);
    }

    @Override
    public int[] getZ() {
        return getField(int[].class, "b", packet);
    }

    @Override
    public int[] getMask() {
        return Arrays.stream(getChunkMap())
                .mapToInt(chunk -> chunk.b)
                .toArray();
    }

    @Override
    public boolean isFull() {
        return getBooleanField("d", packet);
    }

}
