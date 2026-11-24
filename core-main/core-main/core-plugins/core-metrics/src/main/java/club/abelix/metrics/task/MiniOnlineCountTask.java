package club.abelix.metrics.task;

import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.commons.metric.base.MetricGaugeTask;
import club.abelix.network.online.query.StatusQueries;
import club.abelix.network.online.query.execution.StatusQueryExecutionService;
import club.abelix.network.type.mode.ServerMode;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MiniOnlineCountTask extends MetricGaugeTask {

    StatusQueryExecutionService executionService;

    public MiniOnlineCountTask(
            final @NotNull StatusQueryExecutionService executionService
    ) {
        super("mode_online_by_mini",
                "Count of players on minis",
                30, TimeUnit.SECONDS,
                "mini");

        this.executionService = executionService;
    }

    @Override
    public void update() {
        val query = StatusQueries.parametrizedQuery()
                .modes(ServerManager.IMP.findAvailableMinis().stream().map(ServerMode::getName).toList())
                .build();

        executionService.executeAsync(query)
                .thenAccept(res -> {
                    res.getPerServer()
                            .forEach((mode, srv) -> {
                                getCollector().labels(mode).set(srv.getOnline());
                            });
                });
    }

    @Override
    protected Double getValue() {
        return null;
    }
}
