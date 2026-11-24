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
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SSendTitle extends Packet<PlayServerProcessor> {
    private int receiver;
    private String title;
    private String subtitle;
    private int fadeIn;
    private int stay;
    private int fadeOut;

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
        receiver = buf.readInt();
        title = PacketUtils.readString(buf);
        subtitle = PacketUtils.readString(buf);
        fadeIn = PacketUtils.readVarInt(buf);
        stay = PacketUtils.readVarInt(buf);
        fadeOut = PacketUtils.readVarInt(buf);
    }

    @Override
    public void write0(@NotNull ByteBuf buf) throws Exception {
        buf.writeInt(receiver);
        PacketUtils.writeString(buf, title);
        PacketUtils.writeString(buf, subtitle);
        PacketUtils.writeVarInt(buf, fadeIn);
        PacketUtils.writeVarInt(buf, stay);
        PacketUtils.writeVarInt(buf, fadeOut);
    }


}
