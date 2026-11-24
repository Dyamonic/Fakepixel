package club.abelix.network.packet.play.client;

import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketProcessor;
import club.abelix.network.packet.play.processor.PlayClientProcessor;
import club.abelix.network.packet.play.processor.PlayServerProcessor;
import club.abelix.network.packet.play.server.SNetworkBoosterResponse;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * @author milansky
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class CNetworkBoosterRequest extends Packet.Request<PlayClientProcessor, SNetworkBoosterResponse> {
    @Override
    public boolean isProcessor(PacketProcessor processor) {
        return processor instanceof PlayServerProcessor;
    }

    @Override
    protected void read0(@NotNull ByteBuf buf) throws Exception {

    }

    @Override
    protected void write0(@NotNull ByteBuf buf) throws Exception {

    }

    @Override
    public void process(@NotNull PlayClientProcessor processor, @NotNull ChannelProcessorContext ctx) throws Exception {
        processor.process(this, ctx);
    }

    @Override
    public Class<SNetworkBoosterResponse> getResponse() {
        return SNetworkBoosterResponse.class;
    }
}
