package club.abelix.metrics.task;

import club.abelix.commons.metric.base.MetricGaugeTask;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.val;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>PlayerJoinsTask creation on 19.02.2024 at 23:37</p>
 */

@Getter
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class PlayerJoinsTask extends MetricGaugeTask {

    AtomicInteger joins = new AtomicInteger(0);

    public PlayerJoinsTask() {
        super("player_joins",
                "Player joins",
                30, TimeUnit.SECONDS);
    }

    @Override
    protected Double getValue() {
        val value = joins.getAndSet(0);
        return (double) value;
    }
}
