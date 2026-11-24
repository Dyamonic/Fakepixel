package ru.yooxa.http.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import ru.yooxa.concurrent.APICallback;
import ru.yooxa.http.HttpClient;
import ru.yooxa.http.header.Header;
import ru.yooxa.http.header.HttpHeaders;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class NettyHttpClient extends SimpleChannelInboundHandler<HttpResponse> implements HttpClient {

    private static final Class<? extends Channel> CHANNEL_CLASS
            = NettyUtils.getChannelClass();

    private static final EventLoopGroup IO_GROUP
            = NettyUtils.createEventLoopGroup();

    private final InetAddress address;
    private final boolean ssl;
    private final int port;
    private final String host;

    @Setter
    private InetAddress localAddress;

    @Setter
    @Getter
    private int connectionTimeout = 5_000;

    @Setter
    @Getter
    private int readTimeout = 30_000;
    private Channel channel;
    private APICallback<Channel> channelCallback;
    private HttpRequestData request;
    private CompletableFuture<HttpResponse> waitingConsumer;
    private boolean keepAlive = false;
    private long acceptTime;
    private long requestTime;
    public NettyHttpClient(String host, boolean ssl, InetAddress dest) {
        this.ssl = ssl;

        String hostAddr = host;
        int index = host.indexOf(":");

        if (index == -1) {
            this.port = ssl ? 443 : 80;
        } else {
            this.port = Integer.parseInt(host.substring(index + 1));

            hostAddr = host.substring(0, index);
        }

        this.host = hostAddr;

        if (dest == null) {
            try {
                this.address = InetAddress.getByName(this.host);
            } catch (UnknownHostException ex) {
                throw new IllegalStateException("Invalid URL");
            }
        } else {
            this.address = dest;
        }
    }

    private static HttpResponse closedResponse(HttpRequestData request) {
        Map<String, Header> headers = new HashMap<>();
        Header close = new Header(HttpHeaders.CONNECTION, "close");
        headers.put(HttpHeaders.CONNECTION, close);

        HttpResponse response = new HttpResponse(502, "Channel closed", headers, new byte[0]);
        response.request = request;

        return response;
    }

    public void checkConnection(APICallback<Channel> callback) {
        if (this.channel != null) {
            callback.accept(this.channel, null);
            return;
        }

        this.channelCallback = callback;

        final SslContext sslCtx;

        if (this.ssl) {
            try {
                // Использую Deprecated метод, потому что на 1.8 всё обсирается, т.к там устаревшая версия
                // netty. Соответственно в ней нет SslContextBuilder.
                sslCtx = SslContext.newClientContext();
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        } else {
            sslCtx = null;
        }

        new Bootstrap()
                .group(IO_GROUP)
                .option(ChannelOption.TCP_NODELAY, true)
                .channel(CHANNEL_CLASS)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(@NotNull SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();

                        if (sslCtx != null) {
                            //p.addLast(new SslLengthPrepender());
                            p.addLast(sslCtx.newHandler(ch.alloc(), host, port));
                        }

                        p.addLast(new HttpDecoder(), new HttpEncoder());
                        p.addLast(NettyHttpClient.this);
                    }
                })
                .connect(new InetSocketAddress(address, port), new InetSocketAddress(this.localAddress, 0))

                .addListener((ChannelFutureListener) future -> {
                    if (!future.isSuccess()) {
                        callback.accept(null, future.cause());
                    }
                });
    }

    @Override
    public CompletableFuture<HttpResponse> sendRequest(HttpRequestData request) {
        if (this.waitingConsumer != null) {
            throw new IllegalStateException("Waiting response for previous request");
        }

        this.acceptTime = System.currentTimeMillis();

        CompletableFuture<HttpResponse> consumer = new CompletableFuture<>();

        this.waitingConsumer = consumer;

        if (this.channel != null) {
            if (isClosed()) {
                consumer.complete(closedResponse(request));
                return consumer;
            }

            this.keepAlive = true;
            send(request);
        } else {
            checkConnection((channel, e) -> {
                if (e != null) {
                    CompletableFuture<HttpResponse> temp = waitingConsumer;
                    waitingConsumer = null;
                    temp.completeExceptionally(e);
                    return;
                }

                if (isClosed()) {
                    waitingConsumer.complete(closedResponse(request));
                    waitingConsumer = null;
                    return;
                }

                keepAlive = false;
                send(request);
            });
        }

        return consumer;
    }

    private void send(HttpRequestData request) {
        this.request = request;
        this.requestTime = System.currentTimeMillis();
        this.channel.writeAndFlush(request, channel.voidPromise());

        if (this.readTimeout > 0) {
            this.channel.eventLoop().schedule(() -> {
                if (this.request != request) {
                    return;
                }
                if (this.isClosed()) return;

                HttpException exception = new HttpException("Read timeout", request);

                this.channel.pipeline().fireExceptionCaught(exception);
            }, this.readTimeout, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void close() {
        if (channel != null) {
            channel.close();
        }
    }

    private void onClose() {
        if (this.waitingConsumer != null) {
            CompletableFuture<HttpResponse> async = this.waitingConsumer;
            HttpRequestData request = this.request;
            this.waitingConsumer = null;
            this.request = null;

            async.complete(closedResponse(request));
        }
    }

    @Override
    public boolean isClosed() {
        return channel != null && !this.channel.isActive();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpResponse msg) {
        handleResponse(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        CompletableFuture<HttpResponse> consumer = this.waitingConsumer;

        if (consumer != null) {
            this.waitingConsumer = null;
            this.request = null;

            consumer.completeExceptionally(cause);
        }

        ctx.close();
    }

    private void handleResponse(HttpResponse response) {
        CompletableFuture<HttpResponse> consumer = this.waitingConsumer;
        HttpRequestData request = this.request;

        if (consumer != null) {
            this.request = null;
            this.waitingConsumer = null;

            response.keepAlive = this.keepAlive;
            response.acceptTime = this.acceptTime;
            response.requestTime = this.requestTime;
            response.responseTime = System.currentTimeMillis();
            response.request = request;

            consumer.complete(response);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        this.channel = ctx.channel();

        if (this.channelCallback != null) {
            this.channelCallback.accept(this.channel, null);
            this.channelCallback = null;
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        this.onClose();
    }
}
