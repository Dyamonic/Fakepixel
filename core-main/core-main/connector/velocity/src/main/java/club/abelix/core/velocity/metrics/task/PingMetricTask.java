package club.abelix.core.velocity.metrics.task;

import club.abelix.commons.metric.base.MetricGaugeTask;
import club.abelix.core.velocity.gamer.Gamer;
import com.velocitypowered.api.proxy.Player;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class PingMetricTask extends MetricGaugeTask {

    public PingMetricTask() {
        super("velocity_player_ping",
                "Player ping statistics in milliseconds",
                30, TimeUnit.SECONDS,
                "type");
    }


    @Override
    protected void update() {
        List<Long> pings = Gamer.getGamers().stream()
                .map(Gamer::getPlayer)
                .filter(Objects::nonNull)
                .map(Player::getPing)
                .filter(ping -> ping >= 0)
                .sorted()
                .toList();

        if (pings.isEmpty()) {
            getCollector().labels("avg").set(0);
            getCollector().labels("median").set(0);
            getCollector().labels("p95").set(0);
            getCollector().labels("p99").set(0);
            getCollector().labels("min").set(0);
            getCollector().labels("max").set(0);
            return;
        }

        double avg = pings.stream().mapToLong(Long::longValue).average().orElse(0);
        getCollector().labels("avg").set(avg);
        getCollector().labels("median").set(percentile(pings, 50));
        getCollector().labels("p95").set(percentile(pings, 95));
        getCollector().labels("p99").set(percentile(pings, 99));
        getCollector().labels("min").set(pings.getFirst());
        getCollector().labels("max").set(pings.getLast());
    }

    private double percentile(List<Long> sorted, int percentile) {
        if (sorted.size() == 1) {
            return sorted.getFirst();
        }

        double index = (percentile / 100.0) * (sorted.size() - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);
        if (lower == upper) {
            return sorted.get(lower);
        }
        return sorted.get(lower) + (index - lower) * (sorted.get(upper) - sorted.get(lower));
    }

    @Override
    protected Double getValue() {
        return null;
    }
}
