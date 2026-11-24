package club.abelix.network.packet.play.server;

import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketProcessor;
import club.abelix.network.packet.PacketUtils;
import club.abelix.network.packet.play.processor.PlayServerProcessor;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * @author pro0xy
 * <p>Created on 01.11.2023 at 15:13</p>
 */

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SGiveSkyblockGems extends Packet<PlayServerProcessor> {

    private String nick;
    private int gems;

    @Override
    public boolean isProcessor(PacketProcessor processor) {
        return processor instanceof PlayServerProcessor;
    }

    @Override
    protected void read0(@NotNull ByteBuf buf) throws Exception {
        nick = PacketUtils.readString(buf);
        gems = PacketUtils.readVarInt(buf);
    }

    @Override
    protected void write0(@NotNull ByteBuf buf) throws Exception {
        PacketUtils.writeString(buf, nick);
        PacketUtils.writeVarInt(buf, gems);
    }

    @Override
    public void process(@NotNull PlayServerProcessor processor, @NotNull ChannelProcessorContext ctx) throws Exception {
        processor.process(this, ctx);
    }
}
