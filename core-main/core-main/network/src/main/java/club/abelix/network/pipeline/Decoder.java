package club.abelix.network.pipeline;

import club.abelix.network.metrics.PerformanceMetrics;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketDirection;
import club.abelix.network.packet.PacketRegistry;
import club.abelix.network.packet.PacketUtils;
import club.abelix.network.pipeline.exception.BadPacketException;
import club.abelix.network.pipeline.exception.DecoderException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class Decoder extends ByteToMessageDecoder {

    private final PacketDirection direction;
    private PacketRegistry state = PacketRegistry.HANDSHAKE;

    public void upgradeConnection(PacketRegistry newState) {
        state = newState;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() == 0) {
            return;
        }

        try {
            int id = PacketUtils.readVarInt(in);
            Packet<?> packet = direction.getMapper(state).newPacket(id);

            if (packet == null) {
                throw new BadPacketException(id);
            }

            try {
                packet.read(in);
            } catch (Exception e) {
                throw new DecoderException(e);
            }

            out.add(packet);

            PerformanceMetrics.RECEIVED_PACKETS.addValue(1);
        } finally {
            int remaining = in.readableBytes();

            if (remaining > 0) {
                in.skipBytes(remaining);
            }
        }
    }

}