package club.abelix.metrics.task;

import club.abelix.commons.metric.base.MetricGaugeTask;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.val;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: agera-core</p>
 * <p>PlayerLeaveTask creation on 20.06.2024 at 16:08</p>
 */

@Getter
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class PlayerLeaveTask extends MetricGaugeTask {

    AtomicInteger leaves = new AtomicInteger(0);

    public PlayerLeaveTask() {
        super("player_leaves",
                "Player leaves",
                30, TimeUnit.SECONDS);
    }


    @Override
    protected Double getValue() {
        val value = leaves.getAndSet(0);
        return (double) value;
    }
}
