package club.abelix.metrics.task;

import club.abelix.commons.metric.base.MetricGaugeTask;
import club.abelix.network.metrics.PerformanceMetrics;

import java.util.concurrent.TimeUnit;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>CoreTrafficRecivePacketPerSecondTask creation on 25.01.2024 at 02:52</p>
 */

public class CoreTrafficReceivedPacketPerSecondTask extends MetricGaugeTask {
    public CoreTrafficReceivedPacketPerSecondTask() {
        super("core_traffic_packet_received_per_second",
                "Incoming packet per second",
                30, TimeUnit.SECONDS);
    }

    @Override
    protected Double getValue() {
        return PerformanceMetrics.RECEIVED_PACKETS.getSecond();
    }
}
