package club.abelix.network.type.gamer;

import club.abelix.network.packet.PacketUtils;
import club.abelix.network.packet.serialize.Serializable;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;

import java.net.InetAddress;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ReconnectingGamer implements Serializable<ReconnectingGamer> {

    private String name;
    private InetAddress address;

    private String domain;
    private int protocol;

    private String server;

    @Override
    public ReconnectingGamer serialize(ByteBuf out) {
        PacketUtils.writeString(out, name);
        PacketUtils.writeAddress(out, address);
        PacketUtils.writeString(out, server);
        PacketUtils.writeString(out, domain);
        PacketUtils.writeVarInt(out, protocol);

        return this;
    }

    @SneakyThrows
    @Override
    public ReconnectingGamer deserialize(ByteBuf in) {
        name = PacketUtils.readString(in);
        address = PacketUtils.readAddress(in);
        server = PacketUtils.readString(in);
        domain = PacketUtils.readString(in);
        protocol = PacketUtils.readVarInt(in);

        return this;
    }
}
