package club.abelix.core.velocity.metrics.task;

import club.abelix.commons.metric.base.MetricGaugeTask;
import club.abelix.core.velocity.metrics.VelocityMetricsStorage;

import java.util.concurrent.TimeUnit;

public final class PlayerLoginMetricTask extends MetricGaugeTask {

    private final VelocityMetricsStorage storage;

    public PlayerLoginMetricTask(VelocityMetricsStorage storage) {
        super("velocity_player_login",
                "Proxy player login",
                30, TimeUnit.SECONDS);

        this.storage = storage;
    }

    @Override
    protected Double getValue() {
        return (double) storage.getLoginEvent().getAndSet(0);
    }
}
