package club.abelix.network.metrics;

import org.jetbrains.annotations.Contract;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class PacketPerformanceMetrics {

    private static final Map<Class<?>, PacketStat> PACKET_STATS = new ConcurrentHashMap<>();
    private static final Metric GLOBAL_PROCESSING_TIME = new Metric();

    public static void record(Class<?> packetClass, long processingTimeNanos) {
        PACKET_STATS.computeIfAbsent(packetClass, k -> new PacketStat()).record(processingTimeNanos);
        GLOBAL_PROCESSING_TIME.addValue(processingTimeNanos);
    }

    public static Map<Class<?>, PacketStat> getPacketStats() {
        return PACKET_STATS;
    }

    public static double getGlobalAvgProcessingTimeNanos() {
        return GLOBAL_PROCESSING_TIME.getAvgHalfMinute();
    }

    public static class PacketStat {
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
