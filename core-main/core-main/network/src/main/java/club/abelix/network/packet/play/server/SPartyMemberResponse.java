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

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SPartyMemberResponse extends Packet.Response<PlayServerProcessor> {
    private boolean success;
    private String message;
    private List<Integer> members;
    private int leaderId;

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
        success = buf.readBoolean();
        message = PacketUtils.readString(buf);
        members = PacketUtils.readCollection(buf, ArrayList::new, () -> PacketUtils.readVarInt(buf));
        leaderId = PacketUtils.readVarInt(buf);
    }

    @Override
    public void write0(@NotNull ByteBuf buf) throws Exception {
        buf.writeBoolean(success);
        PacketUtils.writeString(buf, message);
        PacketUtils.writeCollection(buf, members, s -> PacketUtils.writeVarInt(buf, s));
        PacketUtils.writeVarInt(buf, leaderId);
    }
}
