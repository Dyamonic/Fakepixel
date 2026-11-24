package club.abelix.games.fastbridge.commons.utils;

import club.abelix.api.base.util.task.AbstractTask;
import club.abelix.games.fastbridge.FastBridge;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class TickTaskExecutor extends AbstractTask {

    private static final TickTaskExecutor INSTANCE = new TickTaskExecutor();

    private final Queue<TickTask> taskQueue = new ConcurrentLinkedQueue<>();
    private boolean active = false;

    public TickTaskExecutor() {
        super(FastBridge.getInstance());
    }

    public static TickTaskExecutor getInstance() {
        return INSTANCE;
    }

    public void addTask(Runnable task, long ticks) {
        taskQueue.add(new TickTask(ticks, task));

        if (!active) {
            start();
        }
    }

    public void addTask(Runnable task) {
        taskQueue.add(new TickTask(1, task));

        if (!active) {
            start();
        }
    }

    public void addTask(Runnable task, long time, TimeUnit unit) {
        addTask(task, unit.toSeconds(time) * 20);
    }

    private void start() {
        if (active) return;

        sync().timer(1, 1);
        active = true;
    }

    public void stop() {
        if (!active) {
            return;
        }

        this.cancel();
        this.active = false;
    }

    @Override
    protected void run() {
        if (taskQueue.isEmpty()) {
            return;
        }

        taskQueue.removeIf(task -> {
            if (--task.tick <= 0) {
                task.runnable.run();
                return true;
            }
            return false;
        });
    }

    private static class TickTask {
        final Runnable runnable;
        long tick;

        public TickTask(long tick, Runnable runnable) {
            this.tick = tick;
            this.runnable = runnable;
        }
    }
}
