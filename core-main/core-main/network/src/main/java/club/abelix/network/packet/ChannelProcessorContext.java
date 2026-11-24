package club.abelix.network.packet;

import club.abelix.network.common.logging.Logging;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.NonNull;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class ChannelProcessorContext {

    @Getter
    private final Packet packet;

    @Getter
    private final BossProcessor mainProcessor;

    @Getter
    private final ChannelHandlerContext context;
    private final AtomicInteger latch = new AtomicInteger();
    private final AtomicBoolean fired = new AtomicBoolean();

    @Getter
    private Packet<?> response;

    public ChannelProcessorContext(Packet<?> packet, BossProcessor mainProcessor, ChannelHandlerContext context) {
        this.packet = packet;
        this.mainProcessor = mainProcessor;
        this.context = context;
    }

    public void addLatch() {
        latch.incrementAndGet();
    }

    public void removeLatch() {
        int remainingLatch = latch.decrementAndGet();
        if (remainingLatch == 0 && fired.get()) {
            done();
        }
    }

    @SuppressWarnings("unchecked")
    public void callProcess(PacketProcessor processor) throws Exception {
        packet.process(processor, this);
    }

    public void setResponse(@NonNull Packet.Response<?> response) {
        this.response = response;

        if (packet instanceof Packet.Request && ((Packet.Request<?, ?>) packet).hasRequestId()) {
            response.setRequestId(((Packet.Request<?, ?>) packet).getRequestId());
        }

    }

    public void setStrictResponse(@NonNull Supplier<Packet.Response<?>> supplier) {
        if (packet instanceof Packet.Request && ((Packet.Request<?, ?>) packet).hasRequestId()) {
            setResponse(supplier.get());
            return;
        }
    }

    public void done() {
        if (response != null) {
            context.writeAndFlush(response);
        }
    }

    public void post() {
        if (latch.get() == 0) {
            done();
        }

        fired.set(true);
    }
}
