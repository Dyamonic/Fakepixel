package club.abelix.api.base.util.task.runner;

import lombok.NonNull;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public interface TaskRunner {

    BukkitTask runTask(@NonNull Plugin plugin, @NonNull Runnable runnable);

    BukkitTask runLater(@NonNull Plugin plugin, @NonNull Runnable runnable, long l);

    BukkitTask runTimer(@NonNull Plugin plugin, @NonNull Runnable runnable, long l, long l1);

}
