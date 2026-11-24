package club.abelix.network.packet.handshake.packet;

import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketProcessor;
import club.abelix.network.packet.PacketUtils;
import club.abelix.network.packet.handshake.Result;
import club.abelix.network.packet.handshake.processor.HandshakeServerProcessor;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>HResponse creation on 18.09.2022 at 01:22</p>
 */


@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class HResponse extends Packet.Response<HandshakeServerProcessor> {
    private Result result;
    private String message;

    public boolean isSuccess() {
        return result == Result.SUCCESS;
    }

    @Override
    public void process(@NotNull HandshakeServerProcessor processor, @NotNull ChannelProcessorContext ctx) throws Exception {
        processor.process(this, ctx);
    }

    @Override
    public boolean isProcessor(PacketProcessor processor) {
        return processor instanceof HandshakeServerProcessor;
    }

    @Override
    public void read0(@NotNull ByteBuf buf) throws Exception {
        result = PacketUtils.readEnum(buf, Result.class);

        if (result == Result.FAILED) {
            message = PacketUtils.readString(buf);
        }
    }

    @Override
    public void write0(@NotNull ByteBuf buf) throws Exception {
        PacketUtils.writeEnum(buf, result);

        if (result == Result.FAILED) {
            PacketUtils.writeString(buf, message);
        }
    }
}
