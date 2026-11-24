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

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
public class SAddServerSuffix extends Packet<PlayServerProcessor> {

    private int suffixId;
    private String text;
    private int suffixColorId;
    private int rarityId;
    private int cost;

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
        suffixId = PacketUtils.readVarInt(buf);
        text = PacketUtils.readString(buf);
        suffixColorId = PacketUtils.readVarInt(buf);
        rarityId = PacketUtils.readVarInt(buf);
        cost = PacketUtils.readVarInt(buf);
    }

    @Override
    public void write0(@NotNull ByteBuf buf) throws Exception {
        PacketUtils.writeVarInt(buf, suffixId);
        PacketUtils.writeString(buf, text);
        PacketUtils.writeVarInt(buf, suffixColorId);
        PacketUtils.writeVarInt(buf, rarityId);
        PacketUtils.writeVarInt(buf, cost);
    }


}
