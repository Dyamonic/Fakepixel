package club.abelix.network.type.report;

import club.abelix.network.packet.PacketUtils;
import club.abelix.network.packet.serialize.Serializable;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PlayerReportSender implements Serializable<PlayerReportSender> {

    private int playerId;
    private ReportReason reason;

    @Override
    public PlayerReportSender serialize(ByteBuf out) {
        out.writeInt(playerId);
        PacketUtils.writeEnum(out, reason, true);

        return this;
    }

    @Override
    public PlayerReportSender deserialize(ByteBuf in) {
        playerId = in.readInt();
        reason = PacketUtils.readEnum(in, ReportReason.class, true);

        return this;
    }
}
