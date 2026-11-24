package club.abelix.metrics.task;

import club.abelix.commons.metric.base.MetricGaugeTask;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.network.type.mode.Mini;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.val;

import java.util.concurrent.TimeUnit;

@Getter
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class ArenaCountTask extends MetricGaugeTask {

    public ArenaCountTask() {
        super("arena_count",
                "Arena count", 30, TimeUnit.SECONDS,
                 "mini", "master");
    }

    ServerManager manager = ServerManager.IMP;


    @Override
    public void update() {
        for (val mini : Mini.VALUES) {
            val serverList = manager.getConnectableServers(mini);

            getCollector().labels(
                    mini.getName(),
                    mini.getMaster().getName()
            ).set(serverList.size());
        }
    }

    @Override
    protected Double getValue() {
        return null;
    }
}
