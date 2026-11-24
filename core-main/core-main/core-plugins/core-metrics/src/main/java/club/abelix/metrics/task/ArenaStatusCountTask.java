package club.abelix.metrics.task;

import club.abelix.commons.metric.base.MetricGaugeTask;
import club.abelix.core.app.connection.server.ArenaServer;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.network.common.enums.GameState;
import lombok.experimental.FieldDefaults;
import lombok.val;

import java.util.concurrent.TimeUnit;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: agera-core</p>
 * <p>ArenaStatusCountTask creation on 20.06.2024 at 16:35</p>
 */

@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class ArenaStatusCountTask extends MetricGaugeTask {

    ServerManager manager = ServerManager.IMP;

    public ArenaStatusCountTask() {
        super("arena_distribute_by_status",
                "Arena distribute by status", 30, TimeUnit.SECONDS,
                "status", "mini", "master");
    }

    @Override
    public void update() {
        for (val mini : manager.findAvailableMinis()) {
            for (val state : GameState.values()) {
                long count = manager.getServers(mini).stream()
                        .filter(abstractServer -> ((ArenaServer) abstractServer).getState() == state)
                        .count();

                getCollector().labels(state.name(),
                        mini.getName(),
                        mini.getMaster().getName()
                ).set(count);
            }
        }

    }

    @Override
    protected Double getValue() {
        return null;
    }
}
