package club.abelix.core.app.connection;

import club.abelix.core.app.CoreApplication;
import club.abelix.network.channel.AbstractRemoteClientChannel;
import club.abelix.network.channel.AbstractServerChannel;
import club.abelix.network.channel.ChannelConstant;
import club.abelix.network.online.query.execution.StatusQueryExecutionService;
import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.socket.SocketChannel;

public class LocalServerConnection extends AbstractServerChannel {
    private final CoreApplication core;
    private final StatusQueryExecutionService executionService;

    public LocalServerConnection(
            StatusQueryExecutionService executionService,
            CoreApplication core,
            String host,
            int port,
            int threads
    ) {
        super(host, port, threads);

        this.executionService = executionService;
        this.core = core;
    }

    @Override
    protected AbstractRemoteClientChannel newClientChannel(SocketChannel channel) {
        return new RemoteClientConnection(
                core,
                channel,
                executionService
        );
    }

    @Override
    protected void initPipeline(SocketChannel channel) {
        super.initPipeline(channel);

        channel.config().setOption(ChannelOption.TCP_FASTOPEN, 3);
        channel.config().setOption(ChannelOption.TCP_NODELAY, true);
        channel.config().setOption(ChannelOption.SO_REUSEADDR, true);
        channel.config().setOption(ChannelOption.SO_KEEPALIVE, true);
        channel.config().setOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, ChannelConstant.CONNECTION_TIMEOUT);
        channel.config().setOption(ChannelOption.SO_LINGER, 0);
        channel.config().setOption(ChannelOption.SO_TIMEOUT, ChannelConstant.SO_TIMEOUT);

        channel.config().setOption(ChannelOption.SO_SNDBUF, 128 * 1024);
        channel.config().setOption(ChannelOption.SO_RCVBUF, 128 * 1024);

        if (isEpoll()) {
            channel.config().setOption(EpollChannelOption.TCP_QUICKACK, true);
        }
    }
}
