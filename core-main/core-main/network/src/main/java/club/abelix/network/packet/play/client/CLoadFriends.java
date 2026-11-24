package club.abelix.network.packet.play.client;

import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketProcessor;
import club.abelix.network.packet.play.processor.PlayClientProcessor;
import club.abelix.network.packet.play.server.SNetworkPlayerLoadResponse;
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
public class CLoadFriends extends Packet.Request<PlayClientProcessor, SNetworkPlayerLoadResponse> {

    private int playerId;

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
    }

    @Override
    public void write0(@NotNull ByteBuf buf) throws Exception {
        buf.writeInt(playerId);
    }


    @Override
    public Class<SNetworkPlayerLoadResponse> getResponse() {
        return SNetworkPlayerLoadResponse.class;
    }
}
