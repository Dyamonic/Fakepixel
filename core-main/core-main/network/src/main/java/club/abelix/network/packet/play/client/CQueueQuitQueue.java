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

/**
 * @author Egor Skorikov (Krashe85)
 * <p>CQueueQuitQueue creation on 17.09.2022 at 23:45</p>
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CQueueQuitQueue extends Packet<PlayClientProcessor> {

    private int playerId;

    @Override
    public boolean isProcessor(PacketProcessor processor) {
        return processor instanceof PlayClientProcessor;
    }

    @Override
    public void read0(@NotNull ByteBuf buf) {
        playerId = PacketUtils.readVarInt(buf);
    }

    @Override
    public void write0(@NotNull ByteBuf buf) {
        PacketUtils.writeVarInt(buf, playerId);
    }

    @Override
    public void process(@NotNull PlayClientProcessor processor, @NotNull ChannelProcessorContext ctx) {
        processor.process(this, ctx);
    }

}
