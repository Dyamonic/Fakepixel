package club.abelix.core.velocity.metrics;

import lombok.Getter;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

public final class VelocityMetricsStorage {

    private final LongAdder serverConnectErrors = new LongAdder();
    private final LongAdder authFailures = new LongAdder();
    private final LongAdder redirectFailures = new LongAdder();

    @Getter
    private final AtomicInteger resetByPeerDisconnect = new AtomicInteger();
    @Getter
    private final AtomicInteger timeoutDisconnect = new AtomicInteger();
    @Getter
    private final AtomicInteger closedChannelsDisconnect = new AtomicInteger();
    @Getter
    private final AtomicInteger otherNettyErrorsDisconnect = new AtomicInteger();

    @Getter
    private final AtomicInteger loginEvent = new AtomicInteger();
    @Getter
    private final AtomicInteger serverMoveEvent = new AtomicInteger();

    @Getter
    private final AtomicInteger pingEvent = new AtomicInteger();

    public void incrementServerConnectErrors() {
        serverConnectErrors.increment();
    }

    public void incrementAuthFailures() {
        authFailures.increment();
    }

    public void incrementRedirectFailures() {
        redirectFailures.increment();
    }

    public long getServerConnectErrors() {
        return serverConnectErrors.sumThenReset();
    }

    public long getAuthFailures() {
        return authFailures.sumThenReset();
    }

    public long getRedirectFailures() {
        return redirectFailures.sumThenReset();
    }
}
