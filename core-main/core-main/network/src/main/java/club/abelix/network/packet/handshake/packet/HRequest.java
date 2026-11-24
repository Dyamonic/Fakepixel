package club.abelix.network.packet.handshake.packet;

import club.abelix.network.common.enums.ConnectionType;
import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketProcessor;
import club.abelix.network.packet.PacketUtils;
import club.abelix.network.packet.handshake.processor.HandshakeClientProcessor;
import club.abelix.network.type.mode.ServerMode;
import club.abelix.network.util.ModeUtil;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>HRequest creation on 18.09.2022 at 01:21</p>
 */

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class HRequest extends Packet.Request<HandshakeClientProcessor, HResponse> {
    private ConnectionType type;

    private String token;

    private String serverName;
    private int serverPort;

    private ServerMode mode;

    @Override
    public void process(@NotNull HandshakeClientProcessor processor, @NotNull ChannelProcessorContext ctx) throws Exception {
        processor.process(this, ctx);
    }

    @Override
    public boolean isProcessor(PacketProcessor processor) {
        return processor instanceof HandshakeClientProcessor;
    }

    @Override
    public void read0(@NotNull ByteBuf buf) throws Exception {
        type = PacketUtils.readEnum(buf, ConnectionType.class);
        token = PacketUtils.readString(buf);
        serverName = PacketUtils.readString(buf);
        serverPort = buf.readUnsignedShort();

        if (type == ConnectionType.SERVER) {
            if (buf.readableBytes() > 0) {
                mode = PacketUtils.readServerMode(buf);
            } else { // support for old servers
                mode = ModeUtil.getServerModeByName(serverName);
            }
        }
    }

    @Override
    public void write0(@NotNull ByteBuf buf) throws Exception {
        PacketUtils.writeEnum(buf, type);
        PacketUtils.writeString(buf, token);
        PacketUtils.writeString(buf, serverName);
        buf.writeShort(serverPort);

        if (type == ConnectionType.SERVER) {
            PacketUtils.writeServerMode(buf, mode);
        }
    }

    @Override
    public Class<HResponse> getResponse() {
        return HResponse.class;
    }
}
