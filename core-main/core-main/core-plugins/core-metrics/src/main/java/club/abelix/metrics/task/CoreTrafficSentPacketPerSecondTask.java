package club.abelix.metrics.task;

import club.abelix.commons.metric.base.MetricGaugeTask;
import club.abelix.network.metrics.PerformanceMetrics;

import java.util.concurrent.TimeUnit;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>CoreTrafficPacketPerSecondTask creation on 25.01.2024 at 02:50</p>
 */

public class CoreTrafficSentPacketPerSecondTask extends MetricGaugeTask {

    public CoreTrafficSentPacketPerSecondTask() {
        super("core_traffic_packet_sent_per_second",
                "Output packet per second",
                30, TimeUnit.SECONDS);
    }

    @Override
    protected Double getValue() {
        return PerformanceMetrics.SENT_PACKETS.getSecond();
    }
}
