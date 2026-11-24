package club.abelix.network.channel;

import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.handshake.processor.HandshakeClientProcessor;
import club.abelix.network.packet.play.processor.PlayClientProcessor;
import io.netty.channel.socket.SocketChannel;
import lombok.NonNull;

public abstract class AbstractRemoteClientChannel extends AbstractRemoteChannel
        implements HandshakeClientProcessor, PlayClientProcessor {

    public AbstractRemoteClientChannel(SocketChannel channel) {
        super(channel);
    }

    @Override
    public void process(@NonNull ChannelProcessorContext ctx) throws Exception {
        ctx.callProcess(this);
        handlePacket(ctx);
    }
}
