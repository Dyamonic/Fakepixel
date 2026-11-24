package club.abelix.network.channel;

import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.handshake.processor.HandshakeServerProcessor;
import club.abelix.network.packet.play.processor.PlayServerProcessor;
import io.netty.channel.socket.SocketChannel;
import lombok.NonNull;

public abstract class AbstractRemoteServerChannel extends AbstractRemoteChannel
        implements HandshakeServerProcessor, PlayServerProcessor {

    private final AbstractClientChannel client;

    public AbstractRemoteServerChannel(AbstractClientChannel client, SocketChannel channel) {
        super(channel);

        this.client = client;
    }

    @Override
    public void process(@NonNull ChannelProcessorContext ctx) throws Exception {
        ctx.callProcess(this);
        handlePacket(ctx);
    }

    @Override
    protected final void onDisconnect() {
        client.reconnect();

        onServerDisconnect();
    }

    protected void onServerDisconnect() {

    }
}
