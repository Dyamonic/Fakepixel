package club.abelix.network.type.gamer;

import club.abelix.network.packet.PacketUtils;
import club.abelix.network.packet.serialize.Serializable;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class FailedReconnectGamer implements Serializable<FailedReconnectGamer> {

    private String name;
    private String reason;

    @Override
    public FailedReconnectGamer serialize(ByteBuf out) {
        PacketUtils.writeString(out, name);
        PacketUtils.writeString(out, reason);

        return this;
    }

    @SneakyThrows
    @Override
    public FailedReconnectGamer deserialize(ByteBuf in) {
        name = PacketUtils.readString(in);
        reason = PacketUtils.readString(in);

        return this;
    }
}
