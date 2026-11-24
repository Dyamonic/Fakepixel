package club.abelix.network.packet.play.client;

import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketProcessor;
import club.abelix.network.packet.play.processor.PlayClientProcessor;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>CPlayerChangeSkin creation on 21.11.2023 at 00:47</p>
 */

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CPlayerChangeSkin extends Packet<PlayClientProcessor> {

    private int playerId;

    @Override
    public void process(
            final @NotNull PlayClientProcessor processor,
            final @NotNull ChannelProcessorContext ctx
    ) throws Exception {
        processor.process(this, ctx);
    }

    @Override
    public boolean isProcessor(final PacketProcessor processor) {
        return processor instanceof PlayClientProcessor;
    }

    @Override
    public void read0(final @NotNull ByteBuf buf) throws Exception {
        playerId = buf.readInt();
    }

    @Override
    public void write0(final @NotNull ByteBuf buf) throws Exception {
        buf.writeInt(playerId);
    }

}
