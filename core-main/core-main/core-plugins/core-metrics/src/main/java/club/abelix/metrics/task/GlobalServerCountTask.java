package club.abelix.metrics.task;

import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.commons.metric.base.MetricGaugeTask;
import lombok.val;

import java.util.concurrent.TimeUnit;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>GlobalServerCount creation on 25.01.2024 at 02:41</p>
 */

public class GlobalServerCountTask extends MetricGaugeTask {
    public GlobalServerCountTask() {
        super("global_server_count",
                "Global count of connected servers",
                30, TimeUnit.SECONDS);
    }

    @Override
    protected Double getValue() {
        val manager = ServerManager.IMP;
        return (double) manager.getServers().size() + manager.getProxies().size();
    }
}

