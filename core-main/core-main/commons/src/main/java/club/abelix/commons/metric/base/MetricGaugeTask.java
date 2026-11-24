package club.abelix.commons.metric.base;

import club.abelix.commons.metric.MetricSimpleTask;
import io.prometheus.client.Gauge;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

@Getter
public abstract class MetricGaugeTask extends MetricSimpleTask<Double> {

    Gauge collector;

    protected MetricGaugeTask(@NotNull String name, @NotNull String help,
                              int delay, @NotNull TimeUnit unit, String... labels) {
        super("gauge_" + name, delay, unit);

        collector = Gauge.build()
                .name(this.getName())
                .help(help)
                .labelNames(labels)
                .register();
    }

    @Override
    protected void update() {
        collector.set(getValue());
    }

}

