package club.abelix.metrics.task;

import club.abelix.commons.metric.base.MetricGaugeTask;
import club.abelix.network.online.query.StatusQueries;
import club.abelix.network.online.query.execution.StatusQueryExecutionService;
import club.abelix.network.type.mode.Master;
import club.abelix.network.type.mode.ServerMode;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>ModeOnlineCountTask creation on 25.01.2024 at 02:58</p>
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MasterOnlineCountTask extends MetricGaugeTask {

    StatusQueryExecutionService executionService;

    public MasterOnlineCountTask(
            final @NotNull StatusQueryExecutionService executionService
    ) {
        super("mode_online_by_master",
                "Count of players on mastests",
                30, TimeUnit.SECONDS,
                "master");

        this.executionService = executionService;
    }

    @Override
    public void update() {
        val query = StatusQueries.parametrizedQuery()
                .modes(Arrays.stream(Master.values()).map(ServerMode::getName).toList())
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
