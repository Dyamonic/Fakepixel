package club.abelix.metrics.task;

import club.abelix.commons.metric.base.MetricGaugeTask;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class JvmGcTask extends MetricGaugeTask {

    private final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

    public JvmGcTask() {
        super("jvm_gc",
                "JVM Garbage Collection metrics",
                30, TimeUnit.SECONDS,
                "gc_name", "metric_type");
    }

    @Override
    public void update() {
        for (GarbageCollectorMXBean bean : gcBeans) {
            getCollector().labels(bean.getName(), "collection_count").set(bean.getCollectionCount());
            getCollector().labels(bean.getName(), "collection_time_ms").set(bean.getCollectionTime());
        }
    }

    @Override
    protected Double getValue() {
        return null;
    }
}
