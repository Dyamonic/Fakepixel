package club.abelix.network.type.report;

import club.abelix.network.packet.PacketUtils;
import club.abelix.network.packet.serialize.Serializable;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PlayerReport implements Serializable<PlayerReport> {

    private int playerId;
    private String server;
    private long lastReportTime;

    private Set<PlayerReportSender> senders;

    @Override
    public PlayerReport serialize(ByteBuf out) {
        out.writeInt(playerId);
        PacketUtils.writeString(out, server);
        out.writeLong(lastReportTime);
        PacketUtils.writeSerializableCollection(out, senders);
        return this;
    }

    @Override
    public PlayerReport deserialize(ByteBuf in) {
        playerId = in.readInt();
        server = PacketUtils.readString(in);
        lastReportTime = in.readLong();
        senders = PacketUtils.readSerializableCollection(in, HashSet::new, PlayerReportSender::new);
        return this;
    }
}
