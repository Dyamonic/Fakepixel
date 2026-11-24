package club.abelix.network.packet.play.client;

import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketProcessor;
import club.abelix.network.packet.PacketUtils;
import club.abelix.network.packet.play.processor.PlayClientProcessor;
import club.abelix.network.type.report.ReportReason;
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
public class CReportAction extends Packet<PlayClientProcessor> {

    private Action action;

    private int playerId;
    private int targetId;
    private ReportReason reason;

    @Override
    public void process(@NotNull PlayClientProcessor processor, @NotNull ChannelProcessorContext ctx) throws Exception {
        processor.process(this, ctx);
    }

    @Override
    public boolean isProcessor(PacketProcessor processor) {
        return processor instanceof PlayClientProcessor;
    }

    @Override
    public void read0(@NotNull ByteBuf buf) throws Exception {
        action = PacketUtils.readEnum(buf, Action.class);
        playerId = PacketUtils.readVarInt(buf);
        targetId = PacketUtils.readVarInt(buf);

        if (action != Action.SEND && action != Action.CLOSE) return;

        if (buf.isReadable()) {
            reason = PacketUtils.readEnum(buf, ReportReason.class);
        }
    }

    @Override
    public void write0(@NotNull ByteBuf buf) throws Exception {
        PacketUtils.writeEnum(buf, action);
        PacketUtils.writeVarInt(buf, playerId);
        PacketUtils.writeVarInt(buf, targetId);

        if (action != Action.SEND && action != Action.CLOSE) return;

        if (reason != null) {
            PacketUtils.writeEnum(buf, reason);
        }
    }

    public enum Action {
        SEND, CLOSE, START_SPECTATING
    }

}
