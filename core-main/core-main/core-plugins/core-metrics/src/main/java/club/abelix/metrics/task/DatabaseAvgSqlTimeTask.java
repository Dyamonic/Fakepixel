package club.abelix.metrics.task;

import club.abelix.commons.metric.base.MetricGaugeTask;
import club.abelix.network.metrics.DatabasePerformanceMetrics;

import java.util.concurrent.TimeUnit;

public class DatabaseAvgSqlTimeTask extends MetricGaugeTask {

    public DatabaseAvgSqlTimeTask() {
        super("database_sql_avg_time_nanos",
                "Average SQL query execution time in nanoseconds over last 10 seconds",
                30, TimeUnit.SECONDS);
    }

    @Override
    protected Double getValue() {
        return DatabasePerformanceMetrics.getGlobalAvgSqlTimeNanos();
    }
}
