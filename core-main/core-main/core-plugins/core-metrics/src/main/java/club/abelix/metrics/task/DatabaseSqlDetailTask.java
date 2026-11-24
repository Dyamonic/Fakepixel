package club.abelix.metrics.task;

import club.abelix.commons.metric.base.MetricGaugeTask;
import club.abelix.network.metrics.DatabasePerformanceMetrics;

import java.util.concurrent.TimeUnit;

public class DatabaseSqlDetailTask extends MetricGaugeTask {

    public DatabaseSqlDetailTask() {
        super("database_sql_detail",
                "SQL query statistics by query template",
                30, TimeUnit.SECONDS,
                "sql_query", "metric_type");
    }

    @Override
    public void update() {
        DatabasePerformanceMetrics.getSqlStats().forEach((sql, stat) -> {
            // We should be careful with label cardinality, but for debugging lags it's useful.
            // Shorten SQL if it's too long
            String shortSql = sql.length() > 100 ? sql.substring(0, 97) + "..." : sql;
            
            getCollector().labels(shortSql, "avg_time_nanos").set(stat.getAvgTimeNanos());
            getCollector().labels(shortSql, "total_count").set(stat.getCount());
        });
    }

    @Override
    protected Double getValue() {
        return null;
    }
}
