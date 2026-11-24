package club.abelix.commons.metric;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public abstract class MetricSimpleTask<T> {
    private static final ScheduledExecutorService SERVICE = Executors.newSingleThreadScheduledExecutor();

    @Getter
    String name;
    long delayMillis;

    protected MetricSimpleTask(@NotNull String name, int delay, @NotNull TimeUnit unit) {
        this.name = "core_" + name;

        delayMillis = unit.toMillis(delay);
    }

    protected abstract void update();

    protected abstract T getValue();

    public void start() {
        SERVICE.scheduleAtFixedRate(this::update, 0,
                delayMillis, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        SERVICE.shutdown();
    }
}
