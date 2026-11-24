package club.abelix.network.packet.play.client;

import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketProcessor;
import club.abelix.network.packet.PacketUtils;
import club.abelix.network.packet.play.processor.PlayClientProcessor;
import club.abelix.network.packet.play.server.SReportCheckStateResponse;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>CReportCheckStateRequest creation on 17.09.2022 at 23:22</p>
 */

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CReportCheckStateRequest extends Packet.Request<PlayClientProcessor, SReportCheckStateResponse> {

    private int playerId;
    private int targetId;

    @Override
    public boolean isProcessor(PacketProcessor processor) {
        return processor instanceof PlayClientProcessor;
    }

    @Override
    protected void read0(@NotNull ByteBuf buf) throws Exception {
        playerId = PacketUtils.readVarInt(buf);
        targetId = PacketUtils.readVarInt(buf);
    }

    @Override
    protected void write0(@NotNull ByteBuf buf) throws Exception {
        PacketUtils.writeVarInt(buf, playerId);
        PacketUtils.writeVarInt(buf, targetId);
    }

    @Override
    public void process(@NotNull PlayClientProcessor processor, @NotNull ChannelProcessorContext ctx) throws Exception {
        processor.process(this, ctx);
    }

    @Override
    public Class<SReportCheckStateResponse> getResponse() {
        return SReportCheckStateResponse.class;
    }
}
