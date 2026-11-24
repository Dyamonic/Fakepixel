package club.abelix.network.type.stream;

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
public class Stream implements Serializable<Stream> {

    private int playerId;
    private String platformName;
    private String title;
    private int viewers;
    private long startedTime;
    private String url;

    @Override
    public Stream serialize(ByteBuf out) {
        PacketUtils.writeVarInt(out, playerId);
        PacketUtils.writeString(out, platformName);
        PacketUtils.writeString(out, title);
        PacketUtils.writeVarInt(out, viewers);
        out.writeLong(startedTime);
        PacketUtils.writeString(out, url);

        return this;
    }

    @Override
    public Stream deserialize(ByteBuf in) {
        playerId = PacketUtils.readVarInt(in);
        platformName = PacketUtils.readString(in);
        title = PacketUtils.readString(in);
        viewers = PacketUtils.readVarInt(in);
        startedTime = in.readLong();
        url = PacketUtils.readString(in);

        return this;
    }
}
