package club.abelix.metrics.task;

import club.abelix.core.app.data.party.manager.PartyManager;
import club.abelix.commons.metric.base.MetricGaugeTask;

import java.util.concurrent.TimeUnit;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>PartyCountTask creation on 25.01.2024 at 02:42</p>
 */

public class PartyCountTask extends MetricGaugeTask {

    public PartyCountTask() {
        super("party_count",
                "Party count",
                30, TimeUnit.SECONDS);
    }

    @Override
    protected Double getValue() {
        return (double) PartyManager.INSTANCE.getParties().size();
    }
}
