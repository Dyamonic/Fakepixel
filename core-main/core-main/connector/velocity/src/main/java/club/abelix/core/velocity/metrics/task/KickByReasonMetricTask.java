package club.abelix.core.velocity.metrics.task;

import club.abelix.commons.metric.base.MetricGaugeTask;
import club.abelix.core.velocity.metrics.KickByReasonMetricsStorage;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public final class KickByReasonMetricTask extends MetricGaugeTask {

    private final KickByReasonMetricsStorage storage;

    public KickByReasonMetricTask(final @NotNull KickByReasonMetricsStorage storage) {
        super(
                "server_kicks_by_reason",
                "Shows amount of kicks by reason",
                30,
                TimeUnit.SECONDS,
                "reason"
        );

        this.storage = storage;
    }

    @Override
    protected void update() {
        storage.getReasonsCountMap().forEach((reason, count) -> getCollector()
                .labels(reason)
                .set(count.sumThenReset()));
    }

    @Override
    protected Double getValue() {
        return null;
    }
}
