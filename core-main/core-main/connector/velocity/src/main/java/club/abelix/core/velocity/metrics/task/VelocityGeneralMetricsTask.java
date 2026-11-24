package club.abelix.core.velocity.metrics.task;

import club.abelix.commons.metric.base.MetricGaugeTask;
import club.abelix.core.velocity.gamer.Gamer;
import club.abelix.core.velocity.metrics.VelocityMetricsStorage;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public final class VelocityGeneralMetricsTask extends MetricGaugeTask {

    private final VelocityMetricsStorage storage;

    public VelocityGeneralMetricsTask(@NotNull VelocityMetricsStorage storage) {
        super("velocity_general",
                "General Velocity metrics",
                30, TimeUnit.SECONDS,
                "metric_type");
        this.storage = storage;
    }


    @Override
    protected void update() {
        getCollector().labels("server_connect_errors").set(storage.getServerConnectErrors());
        getCollector().labels("auth_failures").set(storage.getAuthFailures());
        getCollector().labels("redirect_failures").set(storage.getRedirectFailures());

        val gamers = Gamer.getGamers();
        long connectedToCore = gamers.stream()
                .filter(Gamer::isConnectedToCore)
                .count();
        getCollector().labels("core_connected_players").set(connectedToCore);
        getCollector().labels("total_players").set(gamers.size());
    }

    @Override
    protected Double getValue() {
        return null;
    }
}
