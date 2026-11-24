package club.abelix.network.packet.play.client;

import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketProcessor;
import club.abelix.network.packet.PacketUtils;
import club.abelix.network.packet.play.processor.PlayClientProcessor;
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
public class CSync extends Packet<PlayClientProcessor> {

    private int[] ids;

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
        ids = PacketUtils.readVarIntArray(buf);
    }

    @Override
    public void write0(final @NotNull ByteBuf buf) throws Exception {
        PacketUtils.writeVarIntArray(buf, ids);
    }

}
