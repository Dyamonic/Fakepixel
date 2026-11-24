package club.abelix.core.velocity.metrics.task;

import club.abelix.commons.metric.base.MetricGaugeTask;
import club.abelix.core.velocity.metrics.VelocityMetricsStorage;

import java.util.concurrent.TimeUnit;

public final class PlayerMoveMetricTask extends MetricGaugeTask {

    private final VelocityMetricsStorage storage;

    public PlayerMoveMetricTask(VelocityMetricsStorage storage) {
        super("velocity_player_move",
                "Player move count",
                30, TimeUnit.SECONDS);

        this.storage = storage;
    }

    @Override
    protected Double getValue() {
        return (double) storage.getServerMoveEvent().getAndSet(0);
    }
}
