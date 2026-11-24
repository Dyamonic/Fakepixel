package club.abelix.network.channel;

import club.abelix.network.exception.ConnectException;
import club.abelix.network.packet.BossProcessor;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketDirection;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Getter
@Slf4j
public abstract class AbstractClientChannel extends AbstractChannel {

    protected EventLoopGroup worker;

    protected Class<? extends SocketChannel> channelClass;

    protected Bootstrap bootstrap;
    protected boolean reconnecting;
    private ChannelFuture future;
    private AbstractRemoteServerChannel channel;

    public AbstractClientChannel(String host, int port, int threads) {
        super(host, port, threads);

        init();
    }

    protected void ensureConnectionAvailability() throws ConnectException {
        if (isConnected()) {
            throw new ConnectException(this, new AlreadyConnectedException());
        }
    }

    public void connectAsynchronous() {
        connectAsynchronous(ConnectException::printStackTrace, null);
    }

    public void connectAsynchronous(@NonNull Consumer<ConnectException> errorHandler) {
        connectAsynchronous(errorHandler, null);
    }

    public void reconnect() {
        if (reconnecting) {
            return;
        }

        reconnecting = true;
        scheduleReconnect();
    }

    protected void scheduleReconnect() {
        log.info("Reconnecting in 5 seconds...");
        worker.schedule(this::doReconnect, 5, TimeUnit.SECONDS);
    }

    protected void doReconnect() {
        try {
            connectSynchronized();

            reconnecting = false;
        } catch (Exception e) {
            log.info("Unable to reconnect: {}", String.valueOf(e));

            scheduleReconnect();
        }
    }

    public void connectAsynchronous(@Nullable Runnable success) {
        connectAsynchronous(ConnectException::printStackTrace, success);
    }

    @Override
    public PacketDirection getOutboundPacketDirection() {
        return PacketDirection.TO_SERVER;
    }

    @Override
    public PacketDirection getInboundPacketDirection() {
        return PacketDirection.TO_CLIENT;
    }


    protected abstract AbstractRemoteServerChannel newServerChannel(SocketChannel channel);

    public void connectAsynchronous(@NonNull Consumer<ConnectException> errorHandler, @Nullable Runnable success) {
        try {
            ensureConnectionAvailability();

            future = bootstrap.connect();

            future.addListener(future -> {
                if (future.isSuccess()) {
                    if (success != null) {
                        success.run();
                    }
                } else {
                    errorHandler.accept(new ConnectException(this, future.cause()));
                }

                this.future = null;
            });
        } catch (Exception e) {
            errorHandler.accept(new ConnectException(this, e));
        }
    }

    public void connectSynchronized() throws ConnectException {
        ensureConnectionAvailability();

        try {
            future = bootstrap.connect().sync();
            if (!future.isSuccess()) {
                throw future.cause();
            }
        } catch (Throwable t) {
            throw new ConnectException(this, t);
        } finally {
            future = null;
        }
    }

    protected void closeChannel() {
        channel.close();
    }

    protected void interruptFuture() {
        future.cancel(true);
        future = null;
    }

    public void closeConnection() {
        if (isConnected()) {
            closeChannel();
        } else if (future != null) {
            interruptFuture();
        }

        // Останавливаем экзекьюторы
        worker.shutdownGracefully();
        while (true) {
            try {
                worker.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                break;
            } catch (InterruptedException ignored) {
            }
        }
    }

    public boolean isConnected() {
        return channel != null && channel.isActive();
    }

    public boolean isConnecting() {
        return future != null;
    }

    @Override
    protected void init() {
        try {
            super.init();

            initChannelClass();
            initEventLoopGroups();
            initBootstrap();
        } catch (Exception e) {
            log.error("Failed connection init:", e);
            throw e;
        }
    }

    @Override
    public BossProcessor newPacketProcessor(SocketChannel channel) {
        return this.channel = newServerChannel(channel);
    }

    protected void initChannelClass() {
        channelClass = epoll ? EpollSocketChannel.class : NioSocketChannel.class;
    }

    protected void initEventLoopGroups() {
        worker = epoll
                ? new EpollEventLoopGroup(threads, workerThreadFactory)
                : new NioEventLoopGroup(threads, workerThreadFactory);
    }

    protected void initBootstrap() {
        bootstrap = new Bootstrap()
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.SO_SNDBUF, 64 * 1024)
                .option(ChannelOption.SO_RCVBUF, 64 * 1024)
                .option(ChannelOption.SO_LINGER, 0)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, ChannelConstant.CONNECTION_TIMEOUT * 2)
                .remoteAddress(getConnectAddress())
                .channel(channelClass)
                .handler(channelInitializer)
                .group(worker);
    }

    public void sendPacket(Packet<?> packet) {
        if (isConnected()) {
            channel.sendPacket(packet);
            return;
        }
        log.error("Try send packet, but the connection is not established!");
    }

    public ChannelFuture sendPacketFuture(@NonNull Packet<?> packet) {
        if (!channel.isActive()) {
            return channel.channel.newFailedFuture(new ClosedChannelException());
        }
        return channel.channel.writeAndFlush(packet);
    }


    public <A extends Packet.Response<?>, B extends Packet.Request<?, A>> CompletableFuture<A> awaitPacket(@NonNull B packet) {
        return channel.awaitPacket(packet);
    }

    public <A extends Packet.Response<?>, B extends Packet.Request<?, A>> CompletableFuture<A> awaitPacket(@NonNull B packet, long timeout) {
        return channel.awaitPacket(packet, timeout);
    }
}
