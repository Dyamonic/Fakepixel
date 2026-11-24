package club.abelix.network.metrics;

import lombok.Getter;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.LongAdder;

public class Metric {

    private static final int SECOND_MILLIS = 1000;
    private static final int MINUTE_MILLIS = 60000;
    private static final int HALF_MINUTE_MILLIS = 30000;
    
    // We use 60 buckets, each representing 1 second.
    private static final int BUCKETS = 60;
    private final AtomicLongArray buckets = new AtomicLongArray(BUCKETS);
    private final AtomicLongArray lastUpdateTime = new AtomicLongArray(BUCKETS);
    private final AtomicLongArray eventCounts = new AtomicLongArray(BUCKETS);

    private final LongAdder totalValue = new LongAdder();

    public long getTotalValue() {
        return totalValue.sum();
    }

    public double getSecond() {
        return get(SECOND_MILLIS);
    }

    public double getMinute() {
        return get(MINUTE_MILLIS);
    }

    public double getHalfMinute() {
        return get(HALF_MINUTE_MILLIS);
    }

    public double getAvgSecond() {
        return getAvg(SECOND_MILLIS);
    }

    public double getAvgMinute() {
        return getAvg(MINUTE_MILLIS);
    }

    public double getAvgHalfMinute() {
        return getAvg(HALF_MINUTE_MILLIS);
    }

    public double[] get() {
        return new double[]{
                getSecond(),
                getHalfMinute(),
                getMinute()
        };
    }

    public double[] getAvg() {
        return new double[]{
                getAvgSecond(),
                getAvgHalfMinute(),
                getAvgMinute()
        };
    }

    public void addValue(long value) {
        long now = System.currentTimeMillis();
        long currentSecond = now / 1000;
        int bucketIndex = (int) (currentSecond % BUCKETS);

        long lastUpdate = lastUpdateTime.get(bucketIndex);
        if (lastUpdate != currentSecond) {
            if (lastUpdateTime.compareAndSet(bucketIndex, lastUpdate, currentSecond)) {
                buckets.set(bucketIndex, value);
                eventCounts.set(bucketIndex, 1);
            } else {
                buckets.addAndGet(bucketIndex, value);
                eventCounts.incrementAndGet(bucketIndex);
            }
        } else {
            buckets.addAndGet(bucketIndex, value);
            eventCounts.incrementAndGet(bucketIndex);
        }
        totalValue.add(value);
    }

    public void reset() {
        for (int i = 0; i < BUCKETS; i++) {
            buckets.set(i, 0);
            eventCounts.set(i, 0);
            lastUpdateTime.set(i, 0);
        }
        totalValue.reset();
    }

    public double get(int timeMillis) {
        long now = System.currentTimeMillis();
        long currentSecond = now / 1000;
        long secondsToSum = Math.max(1, timeMillis / 1000);
        long sum = 0;

        for (int i = 0; i < BUCKETS; i++) {
            long lastUpdate = lastUpdateTime.get(i);
            if (currentSecond - lastUpdate < secondsToSum) {
                sum += buckets.get(i);
            }
        }
        
        // If we want rate per second:
        return (double) sum / secondsToSum;
    }

    public double getAvg(int timeMillis) {
        long now = System.currentTimeMillis();
        long currentSecond = now / 1000;
        long secondsToSum = Math.max(1, (long) timeMillis / 1000);
        long sumValue = 0;
        long sumCount = 0;

        for (int i = 0; i < BUCKETS; i++) {
            long lastUpdate = lastUpdateTime.get(i);
            if (currentSecond - lastUpdate < secondsToSum) {
                sumValue += buckets.get(i);
                sumCount += eventCounts.get(i);
            }
        }

        if (sumCount == 0) {
            return 0;
        }

        return (double) sumValue / sumCount;
    }
}