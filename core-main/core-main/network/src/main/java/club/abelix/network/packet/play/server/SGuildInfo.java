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
 * <p>Created on 05.09.2023 at 10:35</p>
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class SGuildInfo extends Packet.Response<PlayServerProcessor> {

    private String guildName;
    private String guildTag;
    private int[] players;

    @Override
    public boolean isProcessor(PacketProcessor processor) {
        return processor instanceof PlayServerProcessor;
    }

    @Override
    protected void read0(@NotNull ByteBuf buf) throws Exception {
        guildName = PacketUtils.readString(buf);
        guildTag = PacketUtils.readString(buf);
        players = PacketUtils.readVarIntArray(buf);
    }

    @Override
    protected void write0(@NotNull ByteBuf buf) throws Exception {
        PacketUtils.writeString(buf, guildName);
        PacketUtils.writeString(buf, guildTag);
        PacketUtils.writeVarIntArray(buf, players);
    }

    @Override
    public void process(@NotNull PlayServerProcessor processor, @NotNull ChannelProcessorContext ctx) throws Exception {
        processor.process(this, ctx);
    }
}
