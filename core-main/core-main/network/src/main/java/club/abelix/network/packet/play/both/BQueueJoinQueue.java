package club.abelix.network.packet.play.both;

import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketProcessor;
import club.abelix.network.packet.PacketUtils;
import club.abelix.network.packet.play.processor.PlayProcessor;
import club.abelix.network.type.mode.Mini;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>BQueueJoinQueue creation on 17.09.2022 at 23:40</p>
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BQueueJoinQueue extends Packet<PlayProcessor> {

    private int playerId;
    private Mini mini;

    @Override
    public boolean isProcessor(PacketProcessor processor) {
        return processor instanceof PlayProcessor;
    }

    @Override
    public void read0(@NotNull ByteBuf buf) {
        playerId = PacketUtils.readVarInt(buf);
        mini = PacketUtils.readEnum(buf, Mini.class, true);
    }

    @Override
    public void write0(@NotNull ByteBuf buf) {
        PacketUtils.writeVarInt(buf, playerId);
        PacketUtils.writeEnum(buf, mini, true);
    }

    @Override
    public void process(@NotNull PlayProcessor processor, @NotNull ChannelProcessorContext ctx) {
        processor.process(this, ctx);
    }

}
