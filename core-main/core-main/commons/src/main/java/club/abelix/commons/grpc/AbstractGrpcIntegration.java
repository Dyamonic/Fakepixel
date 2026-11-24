package club.abelix.commons.grpc;

import io.grpc.*;
import io.grpc.internal.DnsNameResolverProvider;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.channel.EventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.epoll.Epoll;
import io.grpc.netty.shaded.io.netty.channel.epoll.EpollEventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.epoll.EpollSocketChannel;
import io.grpc.netty.shaded.io.netty.channel.nio.NioEventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.socket.nio.NioSocketChannel;
import io.grpc.stub.ClientResponseObserver;
import io.grpc.stub.StreamObserver;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import w.util.Env;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public abstract class AbstractGrpcIntegration {

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.net.preferIPv6Addresses", "false");
    }

    public static final int MAX_INBOUND_MESSAGE_SIZE = Env.getInt("GRPC_MAX_INBOUND_MESSAGE_SIZE", 4 * 1024 * 1024);

    @Getter ExecutorService executor;
    @Getter ManagedChannel channel;

    ScheduledExecutorService scheduler;

    protected AbstractGrpcIntegration(String host, int port) {
        this.executor = Executors.newFixedThreadPool(12, r -> {
            Thread t = new Thread(r, "grpc-offload");
            t.setDaemon(true);
            return t;
        });

        this.scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "grpc-deadline");
            t.setDaemon(true);
            return t;
        });

        NameResolverRegistry.getDefaultRegistry().register(new DnsNameResolverProvider());

        EventLoopGroup group = Epoll.isAvailable()
                ? new EpollEventLoopGroup()
                : new NioEventLoopGroup();

        this.channel = NettyChannelBuilder
                .forAddress(host, port)
                .defaultLoadBalancingPolicy("round_robin")
                .usePlaintext()
                .enableRetry()
                .maxRetryAttempts(3)
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(5, TimeUnit.SECONDS)
                .maxInboundMessageSize(MAX_INBOUND_MESSAGE_SIZE) 
                .eventLoopGroup(group)
                .channelType(Epoll.isAvailable()
                        ? EpollSocketChannel.class
                        : NioSocketChannel.class)
                .build();

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        log.info("Initialized gRPC integration: host={} port={} authority={}", host, port, channel.authority());
        waitChannelReadyOrLog(Env.getInt("GRPC_STARTUP_READY_TIMEOUT_MS", 10_000));
    }

    private void waitChannelReadyOrLog(long timeoutMs) {
        try {
            boolean ok = awaitReady(channel, timeoutMs);
            log.info("gRPC channel READY: {}", ok);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting channel READY");
        }
    }

    private static boolean awaitReady(ManagedChannel ch, long ms) throws InterruptedException {
        long end = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(ms);
        ConnectivityState s = ch.getState(true);
        while (s != ConnectivityState.READY) {
            CountDownLatch latch = new CountDownLatch(1);
            ch.notifyWhenStateChanged(s, latch::countDown);
            long leftNs = end - System.nanoTime();
            if (leftNs <= 0) return false;
            if (!latch.await(Math.max(1, TimeUnit.NANOSECONDS.toMillis(leftNs)), TimeUnit.MILLISECONDS)) return false;
            s = ch.getState(false);
        }
        return true;
    }

    public void shutdown() {
        try {
            if (channel != null) channel.shutdownNow();
            if (scheduler != null) scheduler.shutdownNow();
            if (executor != null) executor.shutdownNow();
        } catch (Exception e) {
            log.error("Error during gRPC integration shutdown", e);
        }
    }

    protected <T, R> CompletableFuture<R> sendUnary(
            T request,
            BiConsumer<T, StreamObserver<R>> grpcCall,
            long timeout,
            TimeUnit unit
    ) {
        CompletableFuture<R> cf = new CompletableFuture<>();
        AtomicBoolean done = new AtomicBoolean(false);

        Context.CancellableContext ctx = Context.ROOT.withDeadlineAfter(timeout, unit, scheduler);
        Context prev = ctx.attach();
        try {
            ClientResponseObserver<T, R> observer = new ClientResponseObserver<>() {
                @Override public void beforeStart(io.grpc.stub.ClientCallStreamObserver<T> req) { /* no-op */ }

                @Override public void onNext(R response) {
                    if (done.compareAndSet(false, true)) {
                        cf.complete(response);
                    }
                }

                @Override public void onError(Throwable t) {
                    if (done.compareAndSet(false, true)) {
                        cf.completeExceptionally(t);
                    }
                }

                @Override public void onCompleted() {
                    if (done.compareAndSet(false, true)) {
                        cf.completeExceptionally(new NoSuchElementException("no response"));
                    }
                }
            };
            grpcCall.accept(request, observer);
        } catch (Throwable t) {
            if (done.compareAndSet(false, true)) {
                cf.completeExceptionally(t);
            }
        } finally {
            ctx.detach(prev);
        }

        cf.whenComplete((r, t) -> {
            if (!ctx.isCancelled()) ctx.cancel(null);
        });

        return cf;
    }

    protected <T, R> CompletableFuture<List<R>> sendServerStreaming(
            T request,
            BiConsumer<T, StreamObserver<R>> grpcCall,
            long timeoutMs
    ) {
        if (!isChannelUsable()) {
            var st = (channel == null) ? null : channel.getState(false);
            return fail("gRPC channel not READY. state=" + st, null);
        }

        CompletableFuture<List<R>> cf = new CompletableFuture<>();
        java.util.concurrent.CopyOnWriteArrayList<R> buf = new java.util.concurrent.CopyOnWriteArrayList<>();

        Context.CancellableContext ctx =
                Context.current().withDeadlineAfter(timeoutMs, TimeUnit.MILLISECONDS, scheduler);
        Context prev = ctx.attach();
        try {
            grpcCall.accept(request, new StreamObserver<>() {
                @Override public void onNext(R r) { buf.add(r); }

                @Override public void onError(Throwable t) {
                    cf.completeExceptionally(t);
                }

                @Override public void onCompleted() {
                    cf.complete(List.copyOf(buf));
                }
            });
        } catch (Throwable t) {
            cf.completeExceptionally(t);
        } finally {
            ctx.detach(prev);
        }

        cf.whenComplete((r, t) -> {
            if (!ctx.isCancelled()) ctx.cancel(null);
        });

        return cf;
    }

    private boolean isChannelUsable() {
        return channel != null && !channel.isShutdown() && !channel.isTerminated();
    }

    private <R> CompletableFuture<R> fail(String msg, Throwable t) {
        return (t == null)
                ? CompletableFuture.failedFuture(new IllegalStateException(msg))
                : CompletableFuture.failedFuture(t);
    }
}
