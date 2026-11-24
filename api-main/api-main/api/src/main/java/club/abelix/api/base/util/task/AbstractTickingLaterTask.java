package club.abelix.api.base.util.task;

import club.abelix.api.base.util.task.cancel.CancelReason;
import club.abelix.api.base.util.task.registration.TaskRegistrationProvider;
import club.abelix.network.common.logging.Logging;
import lombok.NonNull;

public abstract class AbstractTickingLaterTask extends Task<AbstractTickingLaterTask> {

    protected int ticks;
    protected int tick;
    protected int left;
    private int tickDelay;
    private int initialDelay;

    @Deprecated
    public AbstractTickingLaterTask(@NonNull TaskRegistrationProvider provider, @NonNull TaskId taskId) {
        this(provider);
    }

    public AbstractTickingLaterTask(@NonNull TaskRegistrationProvider provider) {
        super(provider);

        this.handleCancel(this::handleCancel);
    }

    protected abstract void runTick();

    protected void handleCancel(@NonNull CancelReason reason) {
    }

    public AbstractTickingLaterTask setTickDelay(int initial, int delay) {
        this.initialDelay = initial;
        this.tickDelay = delay;

        return this;
    }

    public AbstractTickingLaterTask start(int ticks) {
        this.ticks = ticks;

        return timer(initialDelay, tickDelay);
    }

    protected float completePercentage() {
        return (float) left / ticks;
    }

    @Override
    protected void run() {
        if (tick++ >= ticks) {
            cancel(CancelReason.END);
            return;
        }

        left = (ticks - tick) + 1;

        try {
            runTick();
        } catch (Exception any) {
            Logging.IMP.error("[Task] Failed runTick! Details:");
            any.printStackTrace();
        }
    }

    @Override
    protected AbstractTickingLaterTask self() {
        return this;
    }
}
