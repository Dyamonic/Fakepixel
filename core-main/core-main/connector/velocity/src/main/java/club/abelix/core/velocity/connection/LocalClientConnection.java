package club.abelix.core.velocity.connection;


import club.abelix.core.velocity.StarshipVelocity;
import club.abelix.core.velocity.gamer.Gamer;
import club.abelix.network.channel.AbstractClientChannel;
import club.abelix.network.channel.AbstractRemoteServerChannel;
import io.netty.channel.socket.SocketChannel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class LocalClientConnection extends AbstractClientChannel {

    private final StarshipVelocity core;

    public LocalClientConnection(StarshipVelocity core, String host, int port, int threads) {
        super(host, port, threads);

        this.core = core;
    }

    @Override
    protected AbstractRemoteServerChannel newServerChannel(SocketChannel channel) {
        return new RemoteServerConnection(core, channel);
    }

    @Override
    public void reconnect() {
        if (!reconnecting) {
            for (Gamer gamer : Gamer.getGamers()) {
                gamer.sendLocalizedMessage("core_reconnect_message");
            }
        }

        super.reconnect();
    }
}
