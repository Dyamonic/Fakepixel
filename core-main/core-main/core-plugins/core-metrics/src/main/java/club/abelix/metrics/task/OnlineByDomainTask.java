package club.abelix.metrics.task;

import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.commons.metric.base.MetricGaugeTask;
import lombok.val;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>OnlineByDomainTask creation on 20.02.2024 at 01:48</p>
 */

public class OnlineByDomainTask extends MetricGaugeTask {
    public OnlineByDomainTask() {
        super("online_distribute_by_domain",
                "Online users by domains", 30, TimeUnit.SECONDS,
                "domain");
    }

    @Override
    public void update() {
        Map<String, Integer> onlineByDomain = new HashMap<>();

        for (val gamer : Gamer.getGamers()) {
            val domain = gamer.getDomain();

            if (domain == null) {
                continue;
            }

            onlineByDomain.put(domain, onlineByDomain.getOrDefault(domain, 0) + 1);
        }

        onlineByDomain.forEach(
                (domain, online) -> getCollector().labels(domain).set(online));

    }

    @Override
    protected Double getValue() {
        return null;
    }
}
