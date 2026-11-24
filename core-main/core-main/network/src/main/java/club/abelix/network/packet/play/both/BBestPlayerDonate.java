package club.abelix.network.packet.play.both;

import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketProcessor;
import club.abelix.network.packet.play.processor.PlayProcessor;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>BBestPlayerDonate creation on 03.09.2023 at 17:42</p>
 */

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class BBestPlayerDonate extends Packet<PlayProcessor> {

    private int playerId;
    private int donaterId;

    private int golds;

    @Override
    public void process(@NotNull PlayProcessor processor, @NotNull ChannelProcessorContext ctx) throws Exception {
        processor.process(this, ctx);
    }

    @Override
    public boolean isProcessor(PacketProcessor processor) {
        return processor instanceof PlayProcessor;
    }

    @Override
    public void read0(@NonNull ByteBuf buf) throws Exception {
        playerId = buf.readInt();
        donaterId = buf.readInt();
        golds = buf.readInt();
    }

    @Override
    public void write0(@NonNull ByteBuf buf) throws Exception {
        buf.writeInt(playerId);
        buf.writeInt(donaterId);
        buf.writeInt(golds);
    }
}
