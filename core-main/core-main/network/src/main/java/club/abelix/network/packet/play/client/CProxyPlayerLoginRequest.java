package club.abelix.network.packet.play.client;

import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketProcessor;
import club.abelix.network.packet.PacketUtils;
import club.abelix.network.packet.play.processor.PlayClientProcessor;
import club.abelix.network.packet.play.server.SProxyPlayerLoginResponse;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>CProxyPlayerLoginRequest creation on 17.09.2022 at 23:56</p>
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CProxyPlayerLoginRequest extends Packet.Request<PlayClientProcessor, SProxyPlayerLoginResponse> {
    public static final byte VERIFY = 0b00000001;
    public static final byte FINISH = 0b00000010;


    private String name;
    private InetAddress address;

    private String domain;
    private String serverName;

    private int protocol;

    private byte mask;

    public boolean contains(int i) {
        return (mask & i) != 0;
    }


    @Override
    public boolean isProcessor(PacketProcessor processor) {
        return processor instanceof PlayClientProcessor;
    }

    @Override
    public void read0(@NotNull ByteBuf buf) throws Exception {
        name = PacketUtils.readString(buf);

        address = PacketUtils.readAddress(buf);
        mask = buf.readByte();

        if (contains(FINISH)) {
            domain = PacketUtils.readString(buf);
            protocol = PacketUtils.readVarInt(buf);
            serverName = PacketUtils.readString(buf);
        }
    }

    @Override
    public void write0(@NotNull ByteBuf buf) throws Exception {
        PacketUtils.writeString(buf, name);
        PacketUtils.writeAddress(buf, address);
        buf.writeByte(mask);

        if (contains(FINISH)) {
            PacketUtils.writeString(buf, domain);
            PacketUtils.writeVarInt(buf, protocol);
            PacketUtils.writeString(buf, serverName);
        }
    }

    @Override
    public void process(@NotNull PlayClientProcessor processor, @NotNull ChannelProcessorContext ctx) throws Exception {
        processor.process(this, ctx);
    }

    @Override
    public Class<SProxyPlayerLoginResponse> getResponse() {
        return SProxyPlayerLoginResponse.class;
    }

}
