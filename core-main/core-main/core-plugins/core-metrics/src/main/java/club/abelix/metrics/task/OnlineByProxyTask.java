package club.abelix.metrics.task;

import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.commons.metric.base.MetricGaugeTask;
import lombok.experimental.FieldDefaults;
import lombok.val;

import java.util.concurrent.TimeUnit;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>OnlineByProxyTask creation on 19.02.2024 at 23:21</p>
 */

@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class OnlineByProxyTask extends MetricGaugeTask {

    public OnlineByProxyTask() {
        super("online_by_proxy",
                "Online by proxy",
                30, TimeUnit.SECONDS,
                "proxy");

    }

    @Override
    public void update() {
        for (val proxy : ServerManager.IMP.getProxies()) {
            getCollector().labels(proxy.getName()).set(proxy.getOnline());
        }
    }

    @Override
    protected Double getValue() {
        return null;
    }
}
