package club.abelix.network.packet.play.both;

import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketProcessor;
import club.abelix.network.packet.PacketUtils;
import club.abelix.network.packet.play.processor.PlayProcessor;
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
public class BAddServerJoinMessage extends Packet<PlayProcessor> {

    private int joinMessageId;
    private String name;
    private String text;
    private int cost;
    private boolean availableForElite;
    private boolean custom;

    @Override
    public void process(@NotNull PlayProcessor processor, @NotNull ChannelProcessorContext ctx) throws Exception {
        processor.process(this, ctx);
    }

    @Override
    public boolean isProcessor(PacketProcessor processor) {
        return processor instanceof PlayServerProcessor;
    }

    @Override
    public void read0(@NotNull ByteBuf buf) throws Exception {
        joinMessageId = PacketUtils.readVarInt(buf);
        name = PacketUtils.readString(buf);
        text = PacketUtils.readString(buf);
        cost = PacketUtils.readVarInt(buf);
        availableForElite = buf.readBoolean();
        custom = buf.readBoolean();
    }

    @Override
    public void write0(@NotNull ByteBuf buf) throws Exception {
        PacketUtils.writeVarInt(buf, joinMessageId);
        PacketUtils.writeString(buf, name);
        PacketUtils.writeString(buf, text);
        PacketUtils.writeVarInt(buf, cost);
        buf.writeBoolean(availableForElite);
        buf.writeBoolean(custom);
    }


}
