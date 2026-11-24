package club.abelix.network.packet.play.server;

import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketProcessor;
import club.abelix.network.packet.PacketUtils;
import club.abelix.network.packet.play.processor.PlayServerProcessor;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SSendPlayer extends Packet.Response<PlayServerProcessor> {
    private int playerId;
    private int connectionTimeOut;
    private String[] servers;

    @Override
    public void process(@NotNull PlayServerProcessor processor, @NotNull ChannelProcessorContext ctx) throws Exception {
        processor.process(this, ctx);
    }

    @Override
    public boolean isProcessor(PacketProcessor processor) {
        return processor instanceof PlayServerProcessor;
    }

    @Override
    public void read0(@NotNull ByteBuf buf) throws Exception {
        playerId = buf.readInt();
        connectionTimeOut = buf.readInt();
        servers = PacketUtils.readArray(buf, String[]::new, () -> PacketUtils.readString(buf));
    }

    @Override
    public void write0(@NotNull ByteBuf buf) throws Exception {
        buf.writeInt(playerId);
        buf.writeInt(connectionTimeOut);
        PacketUtils.writeArray(buf, servers, server -> PacketUtils.writeString(buf, server));
    }


}
