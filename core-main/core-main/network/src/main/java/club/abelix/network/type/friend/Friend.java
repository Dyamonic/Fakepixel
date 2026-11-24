package club.abelix.network.type.friend;

import club.abelix.network.packet.PacketUtils;
import club.abelix.network.packet.serialize.Serializable;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(
        of = {"playerId"},
        doNotUseGetters = true
)
public class Friend implements Serializable<Friend> {

    private int playerId;
    private String serverName;

    @Override
    public Friend serialize(ByteBuf out) {
        out.writeInt(playerId);
        PacketUtils.writeString(out, serverName);

        return this;
    }

    @Override
    public Friend deserialize(ByteBuf in) {
        playerId = in.readInt();
        serverName = PacketUtils.readString(in);

        return this;
    }
}
