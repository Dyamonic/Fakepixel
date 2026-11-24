package club.abelix.api.base.util.task;

import club.abelix.api.base.util.task.cancel.CancelReason;
import club.abelix.api.base.util.task.registration.TaskRegistrationProvider;
import lombok.NonNull;

public abstract class AbstractTask extends Task<AbstractTask> {

    @Deprecated
    public AbstractTask(@NonNull TaskRegistrationProvider provider, @NonNull TaskId taskId) {
        this(provider);
    }

    public AbstractTask(@NonNull TaskRegistrationProvider provider) {
        super(provider);

        this.handleCancel(this::handleCancel);
    }

    protected abstract void run();

    protected void handleCancel(@NonNull CancelReason reason) {
    }

    public AbstractTask now() {
        return super.now();
    }

    public AbstractTask later(long l) {
        return super.later(l);
    }

    public AbstractTask timer(long l, long l1) {
        return super.timer(l, l1);
    }

    @Override
    protected AbstractTask self() {
        return this;
    }
}
