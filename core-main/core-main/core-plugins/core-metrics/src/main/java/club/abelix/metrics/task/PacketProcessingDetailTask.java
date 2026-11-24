package club.abelix.metrics.task;

import club.abelix.commons.metric.base.MetricGaugeTask;
import club.abelix.network.metrics.PacketPerformanceMetrics;

import java.util.concurrent.TimeUnit;

public class PacketProcessingDetailTask extends MetricGaugeTask {

    public PacketProcessingDetailTask() {
        super("packet_processing_detail",
                "Packet processing statistics by packet type",
                30, TimeUnit.SECONDS,
                "packet_class", "metric_type");
    }

    @Override
    public void update() {
        PacketPerformanceMetrics.getPacketStats().forEach((packetClass, stat) -> {
            String className = packetClass.getSimpleName();
            
            getCollector().labels(className, "avg_time_nanos").set(stat.getAvgTimeNanos());
            getCollector().labels(className, "total_count").set(stat.getCount());
        });
    }

    @Override
    protected Double getValue() {
        return null;
    }
}
