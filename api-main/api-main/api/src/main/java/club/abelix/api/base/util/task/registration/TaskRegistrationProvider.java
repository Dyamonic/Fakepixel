package club.abelix.api.base.util.task.registration;

import club.abelix.api.AbstractPlugin;
import lombok.NonNull;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.UUID;

public interface TaskRegistrationProvider {

    AbstractPlugin getPlugin();

    List<UUID> getRunningTaskIds();

    boolean hasTask(@NonNull UUID taskId);

    void registerTask(@NonNull UUID taskId, @NonNull BukkitTask task);

    boolean removeTask(@NonNull UUID taskId);

    default void removeTasks() {
        getRunningTaskIds().forEach(this::removeTask);
    }
}
