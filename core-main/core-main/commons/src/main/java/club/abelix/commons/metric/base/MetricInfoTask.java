package club.abelix.commons.metric.base;

import club.abelix.commons.metric.MetricSimpleTask;
import io.prometheus.client.Info;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

@Getter
public abstract class MetricInfoTask extends MetricSimpleTask<Void> {

    Info collector;

    protected MetricInfoTask(@NotNull String name, @NotNull String help,
                             int delay, @NotNull TimeUnit unit, String... labels) {
        super("info_" + name, delay, unit);

        collector = Info.build()
                .name(getName())
                .help(help)
                .labelNames(labels)
                .register();
    }

    @Override
    protected abstract void update();

    @Override
    protected final Void getValue() {
        return null;
    }

}

