package club.abelix.api.base.util.task;

import club.abelix.api.base.util.task.cancel.CancelHandler;
import club.abelix.api.base.util.task.cancel.CancelReason;
import club.abelix.api.base.util.task.registration.TaskRegistrationProvider;
import club.abelix.api.base.util.task.runner.AsyncTaskRunner;
import club.abelix.api.base.util.task.runner.SyncTaskRunner;
import club.abelix.api.base.util.task.runner.TaskRunner;
import lombok.NonNull;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Простой баккит таск, при завершении автоматически удаляется из игры
 *
 * @param <TaskType>
 * @see AbstractTask
 * @see AbstractTickingLaterTask
 */
public abstract class Task<TaskType extends Task<TaskType>> {

    protected final TaskRegistrationProvider provider;
    protected final UUID taskId;

    private CancelHandler handler;
    private TaskRunner runner = SyncTaskRunner.INSTANCE;

    public Task(TaskRegistrationProvider provider) {
        this.provider = provider;
        this.taskId = UUID.randomUUID();
    }

    protected void run() {
    }

    public TaskType async() {
        return runner(AsyncTaskRunner.INSTANCE);
    }

    public TaskType sync() {
        return runner(SyncTaskRunner.INSTANCE);
    }

    public TaskType executeOnShutdown() {
        provider.getPlugin().addShutdownTask(this::run);

        return self();
    }

    public TaskType runner(@NonNull TaskRunner runner) {
        this.runner = runner;

        return self();
    }

    private TaskType register(@NonNull BukkitTask task) {
        provider.registerTask(taskId, task);

        return self();
    }

    protected TaskType now() {
        return register(runner.runTask(provider.getPlugin(), () -> {
            run();

            cancel(CancelReason.END); // автоматически удаляем таск из провайдера
        }));
    }

    protected TaskType later(long l) {
        return register(runner.runLater(provider.getPlugin(), () -> {
            run();

            cancel(CancelReason.END); // автоматически удаляем таск из провайдера
        }, l));
    }

    protected TaskType timer(long l, long l1) {
        return register(runner.runTimer(provider.getPlugin(), this::run, l, l1));
    }

    public TaskType handleCancel(@NonNull CancelReason reason, @NonNull Runnable runnable) {
        return handleCancel(handledReason -> {
            if (handledReason == reason) {
                runnable.run();
            }
        });
    }

    public TaskType handleCancel(@NonNull CancelReason reason, @NonNull Consumer<TaskType> taskHandler) {
        return handleCancel(handledReason -> {
            if (handledReason == reason) {
                taskHandler.accept(self());
            }
        });
    }

    public TaskType handleCancel(@NonNull CancelHandler handler) {
        if (this.handler == null) {
            this.handler = handler;
        } else {
            this.handler = this.handler.with(handler);
        }

        return self();
    }

    public TaskType handleCancel(@NonNull Runnable runnable) {
        return handleCancel(reason -> runnable.run());
    }

    public void cancel() {
        cancel(CancelReason.CUSTOM);
    }

    public boolean isExecuting() {
        return provider.hasTask(taskId);
    }

    protected void cancel(@NonNull CancelReason reason) {
        boolean cancelled = provider.removeTask(taskId);

        if (cancelled && handler != null) {
            handler.handleCancel(reason);
        }
    }

    public UUID getTaskId() {
        return taskId;
    }

    protected abstract TaskType self();

}
