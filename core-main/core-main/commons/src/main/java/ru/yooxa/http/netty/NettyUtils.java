package ru.yooxa.http.netty;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * Class of project CryptoPidors on 31.08.2019 in 21:05.
 */
public class NettyUtils {

    private static final int THREADS = 8;
    public static boolean EPOLL = Boolean.parseBoolean(System.getProperty("epoll", "true"));

    public static boolean isEpoll() {
        return EPOLL && Epoll.isAvailable();
    }

    public static EventLoopGroup createEventLoopGroup() {
        return createEventLoopGroup(THREADS);
    }

    public static EventLoopGroup createEventLoopGroup(int n) {
        EventLoopGroup group;

        if (isEpoll()) {
            group = new EpollEventLoopGroup(n);
        } else {
            group = new NioEventLoopGroup(n);
        }

        return group;
    }

    public static Class<? extends Channel> getChannelClass() {
        Class<? extends Channel> channelClass = null;

        if (isEpoll()) {
            channelClass = EpollSocketChannel.class;
        }


        if (channelClass == null) {
            channelClass = NioSocketChannel.class;
        }

        return channelClass;
    }

    public static Class<? extends ServerChannel> getServerChannelClass() {
        Class<? extends ServerChannel> channelClass = null;

        if (isEpoll()) {
            channelClass = EpollServerSocketChannel.class;
        }


        if (channelClass == null) {
            channelClass = NioServerSocketChannel.class;
        }

        return channelClass;
    }
}
