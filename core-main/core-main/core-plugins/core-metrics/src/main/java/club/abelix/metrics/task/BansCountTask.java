package club.abelix.metrics.task;

import club.abelix.core.app.data.punishment.PunishmentManager;
import club.abelix.commons.metric.base.MetricGaugeTask;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.concurrent.TimeUnit;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: agera-core</p>
 * <p>BansCountTask creation on 20.06.2024 at 16:31</p>
 */

@Getter
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class BansCountTask extends MetricGaugeTask {

    PunishmentManager manager = PunishmentManager.INSTANCE;

    public BansCountTask() {
        super("bans",
                "Bans count",
                30, TimeUnit.SECONDS);
    }


    @Override
    protected Double getValue() {
        return (double) manager.getBansCount();
    }
}
