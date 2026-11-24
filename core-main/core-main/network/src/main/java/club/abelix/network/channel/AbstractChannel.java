package club.abelix.network.channel;

import club.abelix.network.common.thread.ThreadUncaughtExceptionHandler;
import club.abelix.network.packet.BossProcessor;
import club.abelix.network.packet.PacketDirection;
import club.abelix.network.pipeline.Pipeline;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.internal.SystemPropertyUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ThreadFactory;

@Getter
@RequiredArgsConstructor
@Slf4j
public abstract class AbstractChannel {

    protected static final boolean epoll = Epoll.isAvailable()
            && SystemPropertyUtil.getBoolean("native.use", true);

    protected static final ThreadFactory bossThreadFactory = new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("[Network] Netty Boss Thread #%1$d")
            .setUncaughtExceptionHandler(new ThreadUncaughtExceptionHandler())
            .build();

    protected static final ThreadFactory workerThreadFactory = new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("[Network] Netty Worker Thread #%1$d")
            .setUncaughtExceptionHandler(new ThreadUncaughtExceptionHandler())
            .build();

    protected final String host;
    protected final int port;
    protected final int threads;
    protected ChannelInitializer<SocketChannel> channelInitializer;

    public abstract BossProcessor newPacketProcessor(SocketChannel channel);

    public abstract PacketDirection getOutboundPacketDirection();

    public abstract PacketDirection getInboundPacketDirection();

    public boolean isEpoll() {
        return epoll;
    }

    protected void initPipeline(SocketChannel channel) {
        Pipeline.initPipeline(this, channel);
    }

    public SocketAddress getConnectAddress() {
        InetSocketAddress address;
        try {
            address = new InetSocketAddress(host, port);
        } catch (SecurityException exception) {
            log.error("Can`t resolve {}. Details: ", host, exception);
            throw new RuntimeException("Can`t  resolve: " + host);
        }

        if (address.isUnresolved()) {
            log.error("Failed resolve address {}", host);
            throw new RuntimeException("Failed resolve address: " + host);
        }

        return address;
    }

    protected void initChannelInitializer() {
        channelInitializer = new ChannelInitializer<>() {
            @Override
            protected void initChannel(@NotNull SocketChannel ch) {
                initPipeline(ch);
            }
        };
    }

    protected void init() {
        initChannelInitializer();
    }

}
