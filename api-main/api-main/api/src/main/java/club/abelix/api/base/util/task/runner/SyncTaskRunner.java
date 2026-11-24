package club.abelix.api.base.util.task.runner;

import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class SyncTaskRunner implements TaskRunner {

    public static final SyncTaskRunner INSTANCE = new SyncTaskRunner();

    private SyncTaskRunner() {
    }

    @Override
    public BukkitTask runTask(@NonNull Plugin plugin, @NonNull Runnable runnable) {
        return Bukkit.getScheduler().runTask(plugin, runnable);
    }

    @Override
    public BukkitTask runLater(@NonNull Plugin plugin, @NonNull Runnable runnable, long l) {
        return Bukkit.getScheduler().runTaskLater(plugin, runnable, l);
    }

    @Override
    public BukkitTask runTimer(@NonNull Plugin plugin, @NonNull Runnable runnable, long l, long l1) {
        return Bukkit.getScheduler().runTaskTimer(plugin, runnable, l, l1);
    }

}
