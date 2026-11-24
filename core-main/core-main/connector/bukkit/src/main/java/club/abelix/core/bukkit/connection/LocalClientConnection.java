package club.abelix.core.bukkit.connection;

import club.abelix.core.bukkit.StarshipBukkit;
import club.abelix.network.channel.AbstractClientChannel;
import club.abelix.network.channel.AbstractRemoteServerChannel;
import io.netty.channel.socket.SocketChannel;
import lombok.Getter;
import lombok.Setter;

public class LocalClientConnection extends AbstractClientChannel {

    private final StarshipBukkit core;
    private final String serverName;

    @Getter
    @Setter
    private boolean firstConnection = true;

    public LocalClientConnection(StarshipBukkit core, String serverName, String host, int port, int threads) {
        super(host, port, threads);

        this.core = core;
        this.serverName = serverName;
    }

    @Override
    protected AbstractRemoteServerChannel newServerChannel(SocketChannel channel) {
        return new RemoteServerConnection(core, serverName, this, channel);
    }

}
