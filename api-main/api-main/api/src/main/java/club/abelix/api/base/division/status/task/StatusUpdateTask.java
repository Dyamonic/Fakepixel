package club.abelix.api.base.division.status.task;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.task.AbstractTask;
import club.abelix.api.base.util.task.cancel.CancelReason;
import club.abelix.network.common.logging.Logging;
import lombok.Getter;
import lombok.NonNull;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author pro0xy
 * <p>Created on 02.09.2023 at 23:37</p>
 */

public class StatusUpdateTask extends AbstractTask {
    // ========
    public static final StatusUpdateTask INSTANCE = new StatusUpdateTask();
    private final CopyOnWriteArrayList<TickExecutorElement> tasksList = new CopyOnWriteArrayList<>();
    // =======
    @Getter
    private boolean active = false;

    public StatusUpdateTask() {
        super(FalconAPI.getInstance());
    }

    public void addTask(Gamer gamer, Runnable execute) {
        tasksList.add(new TickExecutorElement(gamer, execute));

        if (!active) {
            start();
        }
    }

    public void start() {
        if (active) {
            return;
        }
        this.sync().timer(1, 1);
        this.active = true;
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
        for (final TickExecutorElement element : tasksList) {
            try {
                element.getTickExecute().run();
            } catch (Exception e) {
                Logging.IMP.error("StatusUpdate task error:");
                e.printStackTrace();
            }
        }

        if (tasksList.isEmpty()) {
            stop();
        }
    }

    @Override
    protected void handleCancel(@NonNull CancelReason reason) {
        // Выполяем все таски
        tasksList.forEach(task -> {
            try {
                task.getTickExecute().run();
            } catch (Exception e) {
                Logging.IMP.error("StatusUpdate task error:");
                e.printStackTrace();
            }
        });

        tasksList.clear();
        this.active = false;
    }

    public void stopPlayerTask(Gamer gamer){
        tasksList.stream().filter(task -> task.getGamer() == gamer).findFirst().ifPresent(tasksList::remove);
    }
}
