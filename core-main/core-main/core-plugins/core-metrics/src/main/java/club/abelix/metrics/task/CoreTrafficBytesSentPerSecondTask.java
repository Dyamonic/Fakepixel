package club.abelix.metrics.task;

import club.abelix.commons.metric.base.MetricGaugeTask;
import club.abelix.network.metrics.PerformanceMetrics;

import java.util.concurrent.TimeUnit;

public class CoreTrafficBytesSentPerSecondTask extends MetricGaugeTask {

    public CoreTrafficBytesSentPerSecondTask() {
        super("core_traffic_bytes_sent_per_second",
                "Outgoing bytes per second",
                30, TimeUnit.SECONDS);
    }

    @Override
    protected Double getValue() {
        return PerformanceMetrics.TRAFFIC_UPLOAD.getSecond();
    }
}
