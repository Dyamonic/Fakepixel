package club.abelix.network.channel;

import club.abelix.network.packet.BossProcessor;
import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketRegistry;
import club.abelix.network.pipeline.Decoder;
import club.abelix.network.pipeline.Encoder;
import gnu.trove.impl.sync.TSynchronizedIntObjectMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.util.internal.SystemPropertyUtil;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import w.util.Env;

import java.nio.channels.ClosedChannelException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings({"rawtypes", "unchecked"})
@RequiredArgsConstructor
public abstract class AbstractRemoteChannel extends BossProcessor {

    public static final long DEFAULT_TIMEOUT = SystemPropertyUtil.getInt("requestTimeout", 5000);

    protected final SocketChannel channel;

    protected final AtomicInteger requestIdCounter = new AtomicInteger();

    protected final TIntObjectMap<CompletableFuture> responseHandlers
            = new TSynchronizedIntObjectMap<>(new TIntObjectHashMap<>());

    @Getter
    private PacketRegistry protocol;

    protected int nextRequestId() {

        // Возможно ошибка в PacketUtil и он как-то не корректно записывается, хз
        if (requestIdCounter.get() >= 10_000) {
            requestIdCounter.set(0);
        }

        return requestIdCounter.incrementAndGet();
    }

    public void upgradeConnection(PacketRegistry protocol) {
        if (isActive()) {

            channel.pipeline().get(Decoder.class).upgradeConnection(protocol);
            channel.pipeline().get(Encoder.class).upgradeConnection(protocol);

            this.protocol = protocol;
        }
    }

    protected void addResponseHandler(int requestId, @NonNull CompletableFuture handler) {
        responseHandlers.put(requestId, handler);
    }

    protected CompletableFuture<Packet.Response> removeResponseHandler(int requestId) {
        return responseHandlers.remove(requestId);

    }

    protected void handlePacket(@NonNull ChannelProcessorContext ctx) {
        Packet<?> packet = ctx.getPacket();

        if (packet instanceof Packet.Response<?> response) {
            CompletableFuture<Packet.Response> handler = removeResponseHandler(response.getRequestId());

            if (handler == null) {
                return;
            }

            handler.complete(response);
        }
    }

    public boolean isActive() {
        return channel.isActive();
    }

    public void close() {
        if (!isActive()) {
            return;
        }

        channel.close();
    }

    public void sendPacket(@NonNull Packet<?> packet) {
        if (!isActive()) {
            return;
        }

        channel.writeAndFlush(packet, channel.voidPromise());
    }

    public <A extends Packet.Response<?>,
            B extends Packet.Request<?, A>
            > CompletableFuture<A> awaitPacket(@NonNull B packet) {
        return awaitPacket(packet, DEFAULT_TIMEOUT);
    }

    public <A extends Packet.Response<?>,
            B extends Packet.Request<?, A>
            > CompletableFuture<A> awaitPacket(@NonNull B packet,
                                               long timeout) {
        if (!isActive()) {
            throw new IllegalStateException("Channel is closed");
        }

        CompletableFuture<A> response = new CompletableFuture<>();
        int requestId = nextRequestId();

        addResponseHandler(requestId, response);

        packet.setRequestId(requestId);
        sendPacket(packet);

        channel.eventLoop().schedule(() -> {
            CompletableFuture<?> oldHandler = removeResponseHandler(requestId);

            if (oldHandler != null) {
                oldHandler.completeExceptionally(new ReadTimeoutException("Request for packet " +
                        packet.getClass().getSimpleName() + " timed out after " + timeout + "ms"));
            }
        }, timeout, TimeUnit.MILLISECONDS);

        return response;
    }

    @Override
    public final void inactive() {
        protocol = PacketRegistry.HANDSHAKE;

        if (!responseHandlers.isEmpty()) {
            ClosedChannelException exception = new ClosedChannelException();

            responseHandlers.forEachValue(value -> {
                value.completeExceptionally(exception);
                return true;
            });

            responseHandlers.clear();
        }

        onDisconnect();
    }

    protected void onDisconnect() {
        // override me
    }

}
