package club.abelix.metrics.task;

import club.abelix.core.app.data.gamer.GamerPerformanceMetrics;
import club.abelix.commons.metric.base.MetricGaugeTask;

import java.util.concurrent.TimeUnit;

public class GamerLoadAvgTimeTask extends MetricGaugeTask {

    public GamerLoadAvgTimeTask() {
        super("gamer_load_avg_time_millis",
                "Average gamer load time in milliseconds over last 10 seconds",
                30, TimeUnit.SECONDS);
    }

    @Override
    protected Double getValue() {
        return GamerPerformanceMetrics.GAMER_LOAD_TIME.getAvgHalfMinute();
    }
}
