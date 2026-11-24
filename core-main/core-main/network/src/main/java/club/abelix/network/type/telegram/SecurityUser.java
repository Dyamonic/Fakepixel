package club.abelix.network.type.telegram;

import club.abelix.network.packet.PacketUtils;
import club.abelix.network.packet.serialize.Serializable;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class SecurityUser implements Serializable<SecurityUser> {

    private int playerId;

    private InetAddress address;

    private long telegramId;
    private int languageId;

    private String name;
    private String proxyName;

    @Override
    public SecurityUser serialize(ByteBuf out) {
        out.writeInt(playerId);

        PacketUtils.writeAddress(out, address);

        out.writeLong(telegramId);
        out.writeInt(languageId);

        PacketUtils.writeString(out, name);
        PacketUtils.writeString(out, proxyName);

        return this;
    }

    @Override
    public SecurityUser deserialize(ByteBuf in) {
        playerId = in.readInt();

        try {
            address = PacketUtils.readAddress(in);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        telegramId = in.readLong();
        languageId = in.readInt();;

        name = PacketUtils.readString(in);
        proxyName = PacketUtils.readString(in);

        return this;
    }
}