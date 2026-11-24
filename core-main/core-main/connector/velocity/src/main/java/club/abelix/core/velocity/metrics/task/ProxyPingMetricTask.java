package club.abelix.core.velocity.metrics.task;

import club.abelix.commons.metric.base.MetricGaugeTask;
import club.abelix.core.velocity.metrics.VelocityMetricsStorage;

import java.util.concurrent.TimeUnit;

public final class ProxyPingMetricTask extends MetricGaugeTask {

    private final VelocityMetricsStorage storage;

    public ProxyPingMetricTask(VelocityMetricsStorage storage) {
        super("velocity_proxy_ping",
                "Proxy ping count task",
                30, TimeUnit.SECONDS);

        this.storage = storage;
    }

    @Override
    protected Double getValue() {
        return (double) storage.getPingEvent().getAndSet(0);
    }
}
