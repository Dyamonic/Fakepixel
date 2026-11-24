package club.abelix.network.packet.play.server;

import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketProcessor;
import club.abelix.network.packet.PacketUtils;
import club.abelix.network.packet.play.client.CMakePrivateGameResponse;
import club.abelix.network.packet.play.processor.PlayServerProcessor;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class SMakeGamePrivateRequest extends Packet.Request<PlayServerProcessor, CMakePrivateGameResponse> {

    private int partyId;
    private String miniServerName;

    @Override
    public boolean isProcessor(PacketProcessor processor) {
        return processor instanceof PlayServerProcessor;
    }

    @Override
    protected void read0(@NotNull ByteBuf buf) throws Exception {
        partyId = PacketUtils.readVarInt(buf);
        miniServerName = PacketUtils.readString(buf);
    }

    @Override
    protected void write0(@NotNull ByteBuf buf) throws Exception {
        PacketUtils.writeVarInt(buf, partyId);
        PacketUtils.writeString(buf, miniServerName);
    }

    @Override
    public void process(@NotNull PlayServerProcessor processor, @NotNull ChannelProcessorContext ctx) throws Exception {
        processor.process(this, ctx);
    }

    @Override
    public Class<CMakePrivateGameResponse> getResponse() {
        return CMakePrivateGameResponse.class;
    }
}
