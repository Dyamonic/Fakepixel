package club.abelix.api.base.util.task.runner;

import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class AsyncTaskRunner implements TaskRunner {

    public static final AsyncTaskRunner INSTANCE = new AsyncTaskRunner();

    private AsyncTaskRunner() {
    }

    @Override
    public BukkitTask runTask(@NonNull Plugin plugin, @NonNull Runnable runnable) {
        return Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    @Override
    public BukkitTask runLater(@NonNull Plugin plugin, @NonNull Runnable runnable, long l) {
        return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, l);
    }

    @Override
    public BukkitTask runTimer(@NonNull Plugin plugin, @NonNull Runnable runnable, long l, long l1) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, l, l1);
    }

}
