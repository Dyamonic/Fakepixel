package club.abelix.commons.metric.base;

import club.abelix.commons.metric.MetricSimpleTask;
import io.prometheus.client.Histogram;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

@Getter
public abstract class MetricHistogramTask extends MetricSimpleTask<Void> {

    Histogram collector;

    protected MetricHistogramTask(@NotNull String name, @NotNull String help,
                                  int delay, @NotNull TimeUnit unit, @NotNull String... labelName) {
        super("histogram_" + name, delay, unit);

        collector = Histogram.build()
                .name(this.getName())
                .help(help)
                .labelNames(labelName)
                .register();

    }

    @Override
    protected abstract void update();

    @Override
    protected Void getValue() {
        return null;
    }
}
