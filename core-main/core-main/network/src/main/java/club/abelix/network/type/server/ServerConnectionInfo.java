package club.abelix.network.type.server;

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
public class ServerConnectionInfo implements Serializable<ServerConnectionInfo> {

    private String serverName;
    private String serverAddress;
    private int serverPort;

    @Override
    public ServerConnectionInfo serialize(ByteBuf out) {
        PacketUtils.writeString(out, serverName);
        PacketUtils.writeString(out, serverAddress);
        out.writeShort(serverPort);
        return this;
    }

    @Override
    public ServerConnectionInfo deserialize(ByteBuf in) {
        serverName = PacketUtils.readString(in);
        serverAddress = PacketUtils.readString(in);
        serverPort = in.readUnsignedShort();
        return this;
    }
}
