package club.abelix.network.metrics;

import java.util.concurrent.atomic.LongAdder;

public class RedisPerformanceMetrics {

    public static final Metric GLOBAL_REDIS_TIME = new Metric();
    private static final LongAdder OPERATIONS_COUNT = new LongAdder();

    public static void record(long durationNanos) {
        GLOBAL_REDIS_TIME.addValue(durationNanos);
        OPERATIONS_COUNT.increment();
    }

    public static double getAvgRedisTimeMillis() {
        return GLOBAL_REDIS_TIME.getAvgHalfMinute() / 1_000_000.0;
    }
    
    public static long getOperationsCount() {
        return OPERATIONS_COUNT.sum();
    }
}
