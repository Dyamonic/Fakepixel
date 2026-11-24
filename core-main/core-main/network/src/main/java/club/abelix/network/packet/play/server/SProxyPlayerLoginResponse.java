package club.abelix.network.packet.play.server;

import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketProcessor;
import club.abelix.network.packet.PacketUtils;
import club.abelix.network.packet.play.processor.PlayServerProcessor;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>SProxyPlayerLoginResponse creation on 17.09.2022 at 23:53</p>
 */

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SProxyPlayerLoginResponse extends Packet.Response<PlayServerProcessor> {
    private boolean cancelled;
    private String message;

    public SProxyPlayerLoginResponse(boolean cancelled, String message) {
        this.cancelled = cancelled;

        if (cancelled && message == null) {
            message = "";
        }

        this.message = message;
    }

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
        cancelled = buf.readBoolean();

        if (cancelled) {
            message = PacketUtils.readString(buf);
        }
    }

    @Override
    public void write0(@NotNull ByteBuf buf) throws Exception {
        buf.writeBoolean(cancelled);

        if (cancelled) {
            PacketUtils.writeString(buf, message);
        }
    }
}
