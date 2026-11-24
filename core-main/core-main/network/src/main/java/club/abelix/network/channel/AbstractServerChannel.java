package club.abelix.network.channel;

import club.abelix.network.common.logging.Logging;
import club.abelix.network.exception.BindException;
import club.abelix.network.packet.BossProcessor;
import club.abelix.network.packet.PacketDirection;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFactory;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.net.SocketAddress;
import java.nio.channels.AlreadyBoundException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@Getter
public abstract class AbstractServerChannel extends AbstractChannel {

    protected EventLoopGroup boss;
    protected EventLoopGroup worker;

    protected ChannelFactory<ServerSocketChannel> channelFactory;

    protected ServerBootstrap bootstrap;

    private ChannelFuture future;
    private ServerSocketChannel channel;

    public AbstractServerChannel(String host, int port, int threads) {
        super(host, port, threads);

        init();
    }

    protected void checkBindAvailability() throws BindException {
        if (isConnected()) {
            throw new BindException(this, new AlreadyBoundException());
        }
    }

    public void bindAsynchronous() {
        bindAsynchronous(BindException::printStackTrace, null);
    }

    public void bindAsynchronous(@NonNull Consumer<BindException> errorHandler) {
        bindAsynchronous(errorHandler, null);
    }

    public void bindAsynchronous(@Nullable Runnable success) {
        bindAsynchronous(BindException::printStackTrace, success);
    }

    public void bindAsynchronous(@NonNull Consumer<BindException> errorHandler, @Nullable Runnable success) {
        try {
            checkBindAvailability();

            future = bootstrap.bind();

            future.addListener(future -> {
                if (future.isSuccess()) {
                    channel = (ServerSocketChannel) this.future.channel();

                    if (success != null) {
                        success.run();
                    }
                } else {
                    errorHandler.accept(new BindException(this, future.cause()));
                }

                this.future = null;
            });
        } catch (Exception e) {
            errorHandler.accept(new BindException(this, e));
        }
    }

    public void bindSynchronized() throws BindException {
        checkBindAvailability();
        try {
            checkBindAvailability();

            channel = (ServerSocketChannel) bootstrap.bind().addListener(future -> {
                if (future.isSuccess()) {
                    Logging.IMP.info("Server channel success open!");
                } else {
                    future.cause().printStackTrace();
                }
            }).awaitUninterruptibly().channel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void closeChannel() {
        channel.close();
    }

    protected void interruptFuture() {
        future.cancel(true);
        future = null;
    }

    protected abstract AbstractRemoteClientChannel newClientChannel(SocketChannel channel);

    @Override
    public BossProcessor newPacketProcessor(SocketChannel channel) {
        return newClientChannel(channel);
    }

    public void closeConnection() {
        if (isConnected()) {
            closeChannel();
        } else if (future != null) {
            interruptFuture();
        }


        // Останавливаем экзекьюторы
        worker.shutdownGracefully();
        boss.shutdownGracefully();
        while (true) {
            try {
                worker.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                boss.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                break;
            } catch (InterruptedException ignored) {
            }
        }
    }

    public boolean isConnected() {
        return channel != null && channel.isActive();
    }

    @Override
    protected void init() {
        try {
            super.init();

            initChannelFactory();
            initEventLoopGroups();
            initBootstrap();
        } catch (Exception e) {
            log.error("Failed server init: ", e);
            throw e;
        }
    }

    protected void initChannelFactory() {
        channelFactory = epoll ? EpollServerSocketChannel::new : NioServerSocketChannel::new;
    }

    @Override
    public PacketDirection getOutboundPacketDirection() {
        return PacketDirection.TO_CLIENT;
    }

    @Override
    public PacketDirection getInboundPacketDirection() {
        return PacketDirection.TO_SERVER;
    }

    protected void initEventLoopGroups() {
        if (epoll) {
            boss = new EpollEventLoopGroup(1, bossThreadFactory);
            worker = new EpollEventLoopGroup(threads, workerThreadFactory);
        } else {
            boss = new NioEventLoopGroup(1, bossThreadFactory);
            worker = new NioEventLoopGroup(threads, workerThreadFactory);
        }
    }

    protected void initBootstrap() {
        bootstrap = new ServerBootstrap()
                .group(boss, worker)
                .localAddress(getConnectAddress())
                .channelFactory(channelFactory)
                .childHandler(channelInitializer)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.SO_BACKLOG, 512)
                .childOption(ChannelOption.SO_LINGER, 0)
                .childOption(ChannelOption.SO_SNDBUF, 64 * 1024)
                .childOption(ChannelOption.SO_RCVBUF, 64 * 1024)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        if (this.isEpoll()) {
            bootstrap.option(EpollChannelOption.SO_REUSEPORT, true);
            bootstrap.childOption(EpollChannelOption.TCP_QUICKACK, true);
        }
    }

}
