package club.abelix.metrics.task;

import club.abelix.commons.data.network.NetworkSqlHandler;
import club.abelix.commons.metric.base.MetricGaugeTask;
import club.abelix.network.common.logging.Logging;

import java.util.concurrent.TimeUnit;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: agera-core</p>
 * <p>PlayerTotalAccountsTask creation on 20.06.2024 at 16:26</p>
 */

public class PlayerTotalAccountsTask extends MetricGaugeTask {

    public PlayerTotalAccountsTask() {
        super("player_total",
                "Player total",
                30, TimeUnit.SECONDS);
    }

    @Override
    protected Double getValue() {
        try {
            return Double.valueOf(NetworkSqlHandler.INSTANCE.getTotalRegisteredPlayers().get());
        } catch (Exception any) {
            Logging.IMP.error("[Metric] Failed get total players count");
            any.printStackTrace();
            return 0.0;
        }
    }
}