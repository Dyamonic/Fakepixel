package club.abelix.network.packet.play.server;

import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketProcessor;
import club.abelix.network.packet.play.client.CSyncResponse;
import club.abelix.network.packet.play.processor.PlayServerProcessor;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>SSyncRequest creation on 22.11.2023 at 17:58</p>
 */

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Data
public class SSyncRequest extends Packet.Request<PlayServerProcessor, CSyncResponse> {

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
    }

    @Override
    public void write0(@NotNull ByteBuf buf) throws Exception {
    }


    @Override
    public Class<CSyncResponse> getResponse() {
        return CSyncResponse.class;
    }
}

