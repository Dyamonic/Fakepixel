package club.abelix.metrics.task;

import club.abelix.commons.metric.base.MetricGaugeTask;
import club.abelix.network.metrics.PacketPerformanceMetrics;

import java.util.concurrent.TimeUnit;

public class PacketAvgProcessingTimeTask extends MetricGaugeTask {

    public PacketAvgProcessingTimeTask() {
        super("packet_processing_avg_time_nanos",
                "Average packet processing time in nanoseconds over last 10 seconds",
                30, TimeUnit.SECONDS);
    }

    @Override
    protected Double getValue() {
        return PacketPerformanceMetrics.getGlobalAvgProcessingTimeNanos();
    }
}
