package club.abelix.network.packet.play.client;

import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketProcessor;
import club.abelix.network.packet.PacketUtils;
import club.abelix.network.packet.play.processor.PlayClientProcessor;
import club.abelix.network.packet.play.server.SChangePlayerNickResponse;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CChangePlayerNick extends Packet.Request<PlayClientProcessor, SChangePlayerNickResponse> {

    private int playerId;
    private String nick;
    private int groupId;

    public CChangePlayerNick(int playerId) {
        this(playerId, null, 0);
    }

    @Override
    public void process(@NonNull PlayClientProcessor processor, @NotNull ChannelProcessorContext ctx) throws Exception {
        processor.process(this, ctx);
    }

    @Override
    public boolean isProcessor(PacketProcessor processor) {
        return processor instanceof PlayClientProcessor;
    }

    @Override
    public void read0(@NotNull ByteBuf buf) throws Exception {
        playerId = PacketUtils.readVarInt(buf);

        // если ник не отправился, то это значит что игрок его ресетает (/nick reset)
        if (buf.isReadable()) {
            nick = PacketUtils.readString(buf);
            groupId = PacketUtils.readVarInt(buf);
        }
    }

    @Override
    public void write0(@NotNull ByteBuf buf) throws Exception {
        PacketUtils.writeVarInt(buf, playerId);

        if (!isNickReset()) {
            PacketUtils.writeString(buf, nick);
            PacketUtils.writeVarInt(buf, groupId);
        }
    }

    public boolean isNickReset() {
        return nick == null;
    }

    @Override
    public Class<SChangePlayerNickResponse> getResponse() {
        return SChangePlayerNickResponse.class;
    }
}
