package club.abelix.network.type.booster;

import club.abelix.network.packet.PacketUtils;
import club.abelix.network.packet.serialize.Serializable;
import club.abelix.network.type.mode.Master;
import io.netty.buffer.ByteBuf;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.Range;

/**
 * @author milansky
 */
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(
        of = {"owner", "multiplier"},
        doNotUseGetters = true
)
public final class NetworkBooster implements Serializable<NetworkBooster> {

    Master master;
    int owner;
    @Range(from = 0, to = Integer.MAX_VALUE)
    double multiplier;

    @Override
    public NetworkBooster serialize(ByteBuf out) {
        PacketUtils.writeEnum(out, master);
        PacketUtils.writeVarInt(out, owner);
        out.writeDouble(multiplier);
        //сука ты конченная, казлина ты ебанная - https://www.youtube.com/watch?v=Ee6ES9SiPZU
        //PacketUtils.writeString(out, String.valueOf(multiplier));

        return this;
    }

    @Override
    public NetworkBooster deserialize(ByteBuf in) {
        master = PacketUtils.readEnum(in, Master.class);
        owner = PacketUtils.readVarInt(in);
        multiplier = in.readDouble();

        return this;
    }
}
