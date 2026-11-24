package club.abelix.network.pipeline;

import club.abelix.network.metrics.PerformanceMetrics;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketDirection;
import club.abelix.network.packet.PacketRegistry;
import club.abelix.network.packet.PacketUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Encoder extends MessageToByteEncoder<Packet<?>> {

    private final PacketDirection direction;
    private PacketRegistry state = PacketRegistry.HANDSHAKE;

    public void upgradeConnection(PacketRegistry newState) {
        state = newState;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Packet<?> packet, ByteBuf buf) throws Exception {
        // Получаем ID пакета по его классу.
        int packetId = direction.getMapper(state).getPacketId(packet.getClass());

        // Если ID пакета не зарегистрирован, выбрасываем исключение.
        if (packetId == -1) {
            throw new EncoderException("Tried to send unregistered packet: [Packet: " + packet + ", State: " + state + "]");
        }

        // Записываем ID пакета в буфер.
        PacketUtils.writeVarInt(buf, packetId);
        // Записываем данные пакета в буфер.
        packet.write(buf);

        // Увеличиваем метрику производительности для отправленных пакетов.
        PerformanceMetrics.SENT_PACKETS.addValue(1);
    }
}