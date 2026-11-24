package club.abelix.metrics.task;

import club.abelix.commons.metric.base.MetricGaugeTask;
import club.abelix.network.metrics.PerformanceMetrics;

import java.util.concurrent.TimeUnit;

public class CoreTrafficBytesReceivedPerSecondTask extends MetricGaugeTask {

    public CoreTrafficBytesReceivedPerSecondTask() {
        super("core_traffic_bytes_received_per_second",
                "Incoming bytes per second",
                30, TimeUnit.SECONDS);
    }

    @Override
    protected Double getValue() {
        return PerformanceMetrics.TRAFFIC_DOWNLOAD.getSecond();
    }
}
