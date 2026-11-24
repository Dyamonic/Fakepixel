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
 * <p>PlayerMoveTask creation on 20.02.2024 at 00:16</p>
 */

@Getter
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class PlayerMoveTask extends MetricGaugeTask {

    AtomicInteger moves = new AtomicInteger(0);

    public PlayerMoveTask() {
        super("server_moves",
                "Player moves",
                30, TimeUnit.SECONDS);
    }


    @Override
    protected Double getValue() {
        val value = moves.get();
        moves.addAndGet(value * -1);
        return (double) value;
    }
}
