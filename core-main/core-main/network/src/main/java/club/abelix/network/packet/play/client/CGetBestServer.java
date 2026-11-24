package club.abelix.network.packet.play.client;

import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketProcessor;
import club.abelix.network.packet.PacketUtils;
import club.abelix.network.packet.play.processor.PlayClientProcessor;
import club.abelix.network.packet.play.server.SBestServer;
import club.abelix.network.type.mode.ServerMode;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CGetBestServer extends Packet.Request<PlayClientProcessor, SBestServer> {

    private String name;
    private InetAddress address;

    private ServerMode mode;
    private boolean max;

    @Override
    public void process(@NotNull PlayClientProcessor processor, @NotNull ChannelProcessorContext ctx) throws Exception {
        processor.process(this, ctx);
    }

    @Override
    public boolean isProcessor(PacketProcessor processor) {
        return processor instanceof PlayClientProcessor;
    }

    @Override
    public void read0(@NotNull ByteBuf buf) throws Exception {
        this.name = PacketUtils.readString(buf);
        this.address = PacketUtils.readAddress(buf);

        this.mode = PacketUtils.readServerMode(buf);
        this.max = Boolean.parseBoolean(PacketUtils.readString(buf));
    }

    @Override
    public void write0(@NotNull ByteBuf buf) throws Exception {
        PacketUtils.writeString(buf, name);
        PacketUtils.writeAddress(buf, address);

        PacketUtils.writeServerMode(buf, mode);
        PacketUtils.writeString(buf, String.valueOf(max));
    }


    @Override
    public Class<SBestServer> getResponse() {
        return SBestServer.class;
    }
}
