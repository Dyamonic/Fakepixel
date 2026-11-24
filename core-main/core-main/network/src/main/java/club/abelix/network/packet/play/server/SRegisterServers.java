package club.abelix.network.packet.play.server;

import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketProcessor;
import club.abelix.network.packet.PacketUtils;
import club.abelix.network.packet.play.processor.PlayServerProcessor;
import club.abelix.network.type.server.ServerConnectionInfo;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Data
public class SRegisterServers extends Packet<PlayServerProcessor> {

    private ServerConnectionInfo[] servers;

    public SRegisterServers(ServerConnectionInfo... servers) {
        this.servers = servers;
    }

    public SRegisterServers(String name, String address, int port) {
        this(new ServerConnectionInfo(name, address, port));
    }

    @Override
    public void process(@NotNull PlayServerProcessor processor, @NotNull ChannelProcessorContext ctx) throws Exception {
        processor.process(this, ctx);
    }

    @Override
    public boolean isProcessor(PacketProcessor processor) {
        return processor instanceof PlayServerProcessor;
    }

    @Override
    public void read0(@NotNull ByteBuf buf) throws Exception {
        servers = PacketUtils.readSerializableArray(buf, ServerConnectionInfo[]::new, ServerConnectionInfo::new);
    }

    @Override
    public void write0(@NotNull ByteBuf buf) throws Exception {
        PacketUtils.writeSerializableArray(buf, servers);
    }

}
