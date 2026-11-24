package club.abelix.metrics.task;

import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.commons.metric.base.MetricGaugeTask;
import club.abelix.network.type.mode.Mini;
import club.abelix.network.type.mode.Special;
import club.abelix.network.type.mode.Uncommon;

import java.util.concurrent.TimeUnit;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>ServerModeCountServerTask creation on 25.01.2024 at 03:10</p>
 */

public class ServerModeCountServerTask extends MetricGaugeTask {

    private final ServerManager serverManager = ServerManager.IMP;

    public ServerModeCountServerTask() {
        super("mode_server_count",
                "Count of servers",
                30, TimeUnit.SECONDS,
                "mode");
    }

    @Override
    public void update() {
        for (Mini mini : Mini.VALUES) {
            getCollector().labels("mini_" + mini.name()).set(serverManager.getServers(mini).size());
        }

        for (Uncommon uncommon : Uncommon.VALUES) {
            getCollector().labels("uncommon_" + uncommon.name()).set(serverManager.getServers(uncommon).size());
        }

        for (Special special : Special.VALUES) {
            getCollector().labels("special_" + special.name()).set(serverManager.getServers(special).size());
        }
    }

    @Override
    protected Double getValue() {
        return null;
    }
}
