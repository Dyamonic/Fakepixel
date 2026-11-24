package club.abelix.metrics.task;

import club.abelix.commons.metric.base.MetricGaugeTask;
import club.abelix.network.online.query.StatusQueries;
import club.abelix.network.online.query.execution.StatusQueryExecutionService;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;

import java.util.concurrent.TimeUnit;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GlobalOnlineTask extends MetricGaugeTask {

    private final StatusQueryExecutionService queryExecutionService;

    public GlobalOnlineTask(StatusQueryExecutionService queryExecutionService) {
        super("global_online",
                "Global online",
                30, TimeUnit.SECONDS);

        this.queryExecutionService = queryExecutionService;
    }

    @Override
    protected Double getValue() {
        val online = queryExecutionService.executeSync(StatusQueries.totalStatusQuery());

        return (double) online.getTotalOnline();
    }
}
