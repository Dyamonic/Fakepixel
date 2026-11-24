package club.abelix.network.type.map;

import club.abelix.network.packet.PacketUtils;
import club.abelix.network.packet.serialize.Serializable;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>GameMap creation on 25.12.2023 at 23:52</p>
 */

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(
        of = {"technicalName", "multiplier"},
        doNotUseGetters = true
)
public class GameMap implements Serializable<GameMap> {


    private String displayName;
    private String technicalName;
    private int multiplier;

    @Override
    public GameMap serialize(ByteBuf out) {
        PacketUtils.writeString(out, displayName);
        PacketUtils.writeString(out, technicalName);
        out.writeInt(multiplier);

        return this;
    }

    @Override
    public GameMap deserialize(ByteBuf in) {
        displayName = PacketUtils.readString(in);
        technicalName = PacketUtils.readString(in);
        multiplier = in.readInt();

        return this;
    }
}
