package club.abelix.network.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class DatabasePerformanceMetrics {

    private static final Map<String, SQLStat> SQL_STATS = new ConcurrentHashMap<>();
    private static final Metric GLOBAL_SQL_TIME = new Metric();

    public static void record(String sql, long processingTimeNanos) {
        // Normalizing SQL to avoid too many unique labels in Prometheus
        // If SQL has too many unique values (e.g. including IDs), it can cause memory issues.
        // However, here we assume SQL is mostly constant templates.
        SQL_STATS.computeIfAbsent(sql, k -> new SQLStat()).record(processingTimeNanos);
        GLOBAL_SQL_TIME.addValue(processingTimeNanos);
    }

    public static Map<String, SQLStat> getSqlStats() {
        return SQL_STATS;
    }

    public static double getGlobalAvgSqlTimeNanos() {
        return GLOBAL_SQL_TIME.getAvgHalfMinute();
    }

    public static class SQLStat {
        private final LongAdder totalTimeNanos = new LongAdder();
        private final LongAdder count = new LongAdder();

        public void record(long nanos) {
            totalTimeNanos.add(nanos);
            count.increment();
        }

        public long getTotalTimeNanos() {
            return totalTimeNanos.sum();
        }

        public long getCount() {
            return count.sum();
        }

        public double getAvgTimeNanos() {
            long c = count.sum();
            return c == 0 ? 0 : (double) totalTimeNanos.sum() / c;
        }
    }
}
