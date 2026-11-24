package club.abelix.network.packet.play.client;

import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketProcessor;
import club.abelix.network.packet.PacketUtils;
import club.abelix.network.packet.play.processor.PlayClientProcessor;
import club.abelix.network.packet.play.server.SConnectResponse;
import club.abelix.network.type.mode.ServerMode;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Data
public class CConnectMode extends Packet.Request<PlayClientProcessor, SConnectResponse> {

    private int playerId;
    private ServerMode[] modes;

    public CConnectMode(int playerId, ServerMode... modes) {
        this.playerId = playerId;
        this.modes = modes;
    }

    @Override
    public void process(@NotNull PlayClientProcessor processor, @NotNull ChannelProcessorContext ctx) throws Exception {
        processor.process(this, ctx);
    }

    @Override
    public boolean isProcessor(PacketProcessor processor) {
        return processor instanceof PlayClientProcessor;
    }

    @Override
    public void read0(@NotNull ByteBuf buf) throws Exception {
        playerId = buf.readInt();
        modes = PacketUtils.readArray(buf, ServerMode[]::new, () -> PacketUtils.readServerMode(buf));
    }

    @Override
    public void write0(@NotNull ByteBuf buf) throws Exception {
        buf.writeInt(playerId);
        PacketUtils.writeArray(buf, modes, mode -> PacketUtils.writeServerMode(buf, mode));
    }

    @Override
    public Class<SConnectResponse> getResponse() {
        return SConnectResponse.class;
    }
}
