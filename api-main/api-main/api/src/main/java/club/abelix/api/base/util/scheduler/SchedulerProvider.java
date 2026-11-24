package club.abelix.api.base.util.scheduler;

import lombok.NonNull;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

public interface SchedulerProvider<R> {

    R runTaskLaterSync(
            @NonNull Runnable runnable,
            long delay,
            @NonNull TimeUnit unit
    );

    R runTaskLaterAsync(
            @NonNull Runnable runnable,
            long delay,
            @NonNull TimeUnit unit
    );

    R runTaskTimerSync(
            @NonNull Runnable runnable,
            long initial,
            long delay,
            @NonNull TimeUnit unit
    );

    R runTaskTimerAsync(
            @NonNull Runnable runnable,
            long initial,
            long delay,
            @NonNull TimeUnit unit
    );

    R runTaskLaterSync(
            @NonNull Object link,
            @NonNull Runnable runnable,
            long delay,
            @NonNull TimeUnit unit
    );

    R runTaskLaterAsync(
            @NonNull Object link,
            @NonNull Runnable runnable,
            long delay,
            @NonNull TimeUnit unit
    );

    R runTaskTimerSync(
            @NonNull Object link,
            @NonNull Runnable runnable,
            long initial,
            long delay,
            @NonNull TimeUnit unit
    );

    R runTaskTimerAsync(
            @NonNull Object link,
            @NonNull Runnable runnable,
            long initial,
            long delay,
            @NonNull TimeUnit unit
    );

    Collection<R> getLinkedTasks(@NonNull Object link);

    void cancelTask(@NonNull R task);

}
