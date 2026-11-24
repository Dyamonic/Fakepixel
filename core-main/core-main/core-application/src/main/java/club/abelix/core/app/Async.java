package club.abelix.core.app;

import lombok.NonNull;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Async {

    public static void run(@NonNull Runnable task) {
        CoreApplication.getInstance().getExecutor().submit(task);
    }

    public static ScheduledFuture<?> runLater(@NonNull Runnable task, long delay, @NonNull TimeUnit unit) {
        return CoreApplication.getInstance().getScheduler().schedule(task, delay, unit);
    }

    public static ScheduledFuture<?> runTimer(@NonNull Runnable task, long initial, long delay, @NonNull TimeUnit unit) {
        return CoreApplication.getInstance().getScheduler().scheduleWithFixedDelay(task, initial, delay, unit);
    }
}
