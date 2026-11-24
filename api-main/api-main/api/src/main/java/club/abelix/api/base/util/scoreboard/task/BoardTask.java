package club.abelix.api.base.util.scoreboard.task;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.util.task.AbstractTask;
import club.abelix.network.common.logging.Logging;
import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.NonNull;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>BoardTask creation on 28.02.2023 at 12:27</p>
 */

public class BoardTask extends AbstractTask {
    @Getter
    private static final BoardTask instance = new BoardTask();

    private static final Set<BoardUpdater> TASKS = Sets.newConcurrentHashSet();

    private boolean run = false;

    public BoardTask() {
        super(FalconAPI.getInstance());
    }

    public void addUpdater(@NonNull BoardUpdater updater) {
        TASKS.add(updater);

        if (!run) {
            start();
        }
    }

    public void removeUpdater(@NonNull BoardUpdater updater) {
        TASKS.remove(updater);

        if (TASKS.isEmpty() && run) {
            stop();
        }
    }

    public void start() {
        if (run) {
            return;
        }

        this.async().timer(0, 1);
        this.run = true;
    }

    public void stop() {
        if (!run) {
            return;
        }

        this.cancel();
        this.run = false;
    }

    @Override
    protected void run() {
        // Обычный foreach, чтобы экзепшен не застрял в лямбде
        Set<BoardUpdater> needRemove = new HashSet<>();

        for (BoardUpdater updater : TASKS) {
            try {
                updater.updateBoardReceivers();

                if (!updater.getBoard().haveBoardPlayers()) {
                    needRemove.add(updater);
                    continue;
                }

                updater.doTick();
            } catch (Exception any) {
                Logging.IMP.error("BoardUpdateTask: Failed update. Details:");
                any.printStackTrace();
            }
        }

        for (BoardUpdater need : needRemove) {
            removeUpdater(need);
        }
    }
}
