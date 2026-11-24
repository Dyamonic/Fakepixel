package club.abelix.core.velocity.metrics.task;

import club.abelix.commons.metric.base.MetricGaugeTask;
import club.abelix.core.velocity.metrics.VelocityMetricsStorage;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public final class DisconnectNettyMetricTask extends MetricGaugeTask {

    private final VelocityMetricsStorage storage;

    public DisconnectNettyMetricTask(@NotNull VelocityMetricsStorage storage) {
        super("velocity_netty_disconnect",
                "Incorrect Disconnect",
                30, TimeUnit.SECONDS,
                "type");
        this.storage = storage;
    }

    @Override
    protected void update() {
        getCollector().labels("reset_by_peer").set(storage.getResetByPeerDisconnect().getAndSet(0));
        getCollector().labels("timeouts").set(storage.getTimeoutDisconnect().getAndSet(0));
        getCollector().labels("closed_channel").set(storage.getClosedChannelsDisconnect().getAndSet(0));
        getCollector().labels("other").set(storage.getOtherNettyErrorsDisconnect().getAndSet(0));
    }

    @Override
    protected Double getValue() {
        return null;
    }
}