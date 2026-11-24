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
public class SMuteChangeMuteState extends Packet<PlayServerProcessor> {

    private int muteId;
    private int playerId;
    private String admin;
    private String reason;
    private long issueDate;
    private long expiration;

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
        muteId = buf.readInt();
        playerId = buf.readInt();
        admin = PacketUtils.readString(buf);
        reason = PacketUtils.readString(buf);
        issueDate = buf.readLong();
        expiration = buf.readLong();
    }

    @Override
    public void write0(@NotNull ByteBuf buf) throws Exception {
        buf.writeInt(muteId);
        buf.writeInt(playerId);
        PacketUtils.writeString(buf, admin);
        PacketUtils.writeString(buf, reason);
        buf.writeLong(issueDate);
        buf.writeLong(expiration);
    }


}
