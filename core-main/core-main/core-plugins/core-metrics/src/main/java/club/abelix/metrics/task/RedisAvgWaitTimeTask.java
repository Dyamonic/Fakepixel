package club.abelix.metrics.task;

import club.abelix.commons.metric.base.MetricGaugeTask;
import club.abelix.network.metrics.RedisPerformanceMetrics;

import java.util.concurrent.TimeUnit;

public class RedisAvgWaitTimeTask extends MetricGaugeTask {

    public RedisAvgWaitTimeTask() {
        super("redis_avg_wait_time_millis",
                "Average time to get a Redis connection from the pool in milliseconds over last 10 seconds",
                30, TimeUnit.SECONDS);
    }

    @Override
    protected Double getValue() {
        return RedisPerformanceMetrics.getAvgRedisTimeMillis();
    }
}
