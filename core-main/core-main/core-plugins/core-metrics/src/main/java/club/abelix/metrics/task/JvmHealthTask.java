package club.abelix.metrics.task;

import club.abelix.commons.metric.base.MetricGaugeTask;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.TimeUnit;

public class JvmHealthTask extends MetricGaugeTask {

    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    public JvmHealthTask() {
        super("jvm_health",
                "Basic JVM health metrics",
                30, TimeUnit.SECONDS,
                "metric_type");
    }

    @Override
    public void update() {
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        getCollector().labels("heap_used_bytes").set(heapUsage.getUsed());
        getCollector().labels("heap_max_bytes").set(heapUsage.getMax());

        MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();
        getCollector().labels("non_heap_used_bytes").set(nonHeapUsage.getUsed());

        getCollector().labels("thread_count").set(threadMXBean.getThreadCount());
        getCollector().labels("daemon_thread_count").set(threadMXBean.getDaemonThreadCount());
    }

    @Override
    protected Double getValue() {
        return null;
    }
}
