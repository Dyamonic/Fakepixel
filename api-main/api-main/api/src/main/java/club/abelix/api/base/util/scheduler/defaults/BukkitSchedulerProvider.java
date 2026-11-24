package club.abelix.api.base.util.scheduler.defaults;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.util.scheduler.SchedulerProvider;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class BukkitSchedulerProvider implements SchedulerProvider<BukkitTask> {

    Map<Object, List<BukkitTask>> links = Maps.newConcurrentMap();
    AbstractPlugin plugin;

    @Override
    public BukkitTask runTaskLaterSync(
            @NonNull Runnable runnable,
            long delay,
            @NonNull TimeUnit unit
    ) {
        return runTaskLaterSync(plugin, runnable, delay, unit);
    }

    @Override
    public BukkitTask runTaskLaterAsync(
            @NonNull Runnable runnable,
            long delay,
            @NonNull TimeUnit unit
    ) {
        return runTaskLaterAsync(plugin, runnable, delay, unit);
    }

    @Override
    public BukkitTask runTaskTimerSync(
            @NonNull Runnable runnable,
            long initial,
            long delay,
            @NonNull TimeUnit unit) {
        return runTaskTimerSync(plugin, runnable, initial, delay, unit);
    }

    @Override
    public BukkitTask runTaskTimerAsync(
            @NonNull Runnable runnable,
            long initial,
            long delay,
            @NonNull TimeUnit unit
    ) {
        return runTaskTimerAsync(plugin, runnable, initial, delay, unit);
    }

    @Override
    public BukkitTask runTaskLaterSync(
            @NonNull Object link,
            @NonNull Runnable runnable,
            long delay,
            @NonNull TimeUnit unit) {
        var list = links.computeIfAbsent(link, __ -> Lists.newArrayList());
        var task = Bukkit.getScheduler().runTaskLater(
                plugin,
                runnable,
                unit.toSeconds(delay) * 20L
        );

        list.add(task);

        return task;
    }

    @Override
    public BukkitTask runTaskLaterAsync(
            @NonNull Object link,
            @NonNull Runnable runnable,
            long delay,
            @NonNull TimeUnit unit
    ) {
        var list = links.computeIfAbsent(link, __ -> Lists.newArrayList());
        var task = Bukkit.getScheduler().runTaskLaterAsynchronously(
                plugin,
                runnable,
                unit.toSeconds(delay) * 20L
        );

        list.add(task);

        return task;
    }

    @Override
    public BukkitTask runTaskTimerSync(
            @NonNull Object link,
            @NonNull Runnable runnable,
            long initial,
            long delay,
            @NonNull TimeUnit unit
    ) {
        var list = links.computeIfAbsent(link, __ -> Lists.newArrayList());
        var task = Bukkit.getScheduler().runTaskTimer(
                plugin, runnable,
                unit.toSeconds(initial) * 20L,
                unit.toSeconds(delay) * 20L
        );

        list.add(task);

        return task;
    }

    @Override
    public BukkitTask runTaskTimerAsync(
            @NonNull Object link,
            @NonNull Runnable runnable,
            long initial,
            long delay,
            @NonNull TimeUnit unit
    ) {
        var list = links.computeIfAbsent(link, __ -> Lists.newArrayList());
        var task = Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin, runnable,
                unit.toSeconds(initial) * 20L,
                unit.toSeconds(delay) * 20L
        );

        list.add(task);

        return task;
    }

    @Override
    public Collection<BukkitTask> getLinkedTasks(@NonNull Object link) {
        return links.getOrDefault(link, Lists.newArrayList());
    }

    @Override
    public void cancelTask(@NonNull BukkitTask task) {
        task.cancel();
    }
}
