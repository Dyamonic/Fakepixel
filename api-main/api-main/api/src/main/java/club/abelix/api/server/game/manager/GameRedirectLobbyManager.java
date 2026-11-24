package club.abelix.api.server.game.manager;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.task.AbstractTask;
import club.abelix.api.base.util.task.cancel.CancelReason;
import club.abelix.network.common.logging.Logging;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-api</p>
 * <p>GameLeaveManager creation on 01.02.2024 at 00:36</p>
 */

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class GameRedirectLobbyManager {
    // ================================  INSTANCE  ======================================= //
    public final static GameRedirectLobbyManager INSTANCE = new GameRedirectLobbyManager();
    // =================================================================================== //

    static Map<Gamer, GameRedirectLobbyTask.TaskUnit> waitSend = new ConcurrentHashMap<>();

    static AtomicBoolean taskRun = new AtomicBoolean(false);

    GameRedirectLobbyTask task = new GameRedirectLobbyTask();

    public boolean isWaitSend(@NonNull Gamer gamer) {
        return waitSend.containsKey(gamer);
    }

    public void add(@NonNull Gamer gamer, @NonNull Runnable runnable, int seconds) {
        waitSend.put(gamer, new GameRedirectLobbyTask.TaskUnit(gamer, runnable, seconds));
        gamer.sendLocalizedMessage("redirect_lobby_delay", "%delay%", seconds);
        trigger();
    }

    public void remove(@NonNull Gamer gamer) {
        remove(gamer, false);
    }

    public void remove(@NonNull Gamer gamer, boolean unload) {
        waitSend.remove(gamer);
        trigger();

        if (!unload) {
            gamer.sendLocalizedMessage("redirect_lobby_cancel");
        }
    }

    // ==== Private section ====
    private void trigger() {
        if (waitSend.isEmpty() && taskRun.get()) {
            task.cancel();
        } else if (!taskRun.get()) {
            taskRun.set(true);
            task.timer(20L, 20L);
        }
    }

    private static final class GameRedirectLobbyTask extends AbstractTask {

        GameRedirectLobbyTask() {
            super(FalconAPI.getInstance());
        }

        @Override
        protected void run() {
            if (waitSend.isEmpty()) {
                cancel(CancelReason.END);
                return;
            }

            // Копируем, для того чтобы избежать ConcurrentModificationException
            // Я не уверен, что она может тут вылезти, но на всякий
            Set<TaskUnit> itterationSet = new HashSet<>(waitSend.values());
            for (val unit : itterationSet) {
                if (unit.isDelay()) {
                    try {
                        unit.runnable.run();
                    } catch (Exception any) {
                        Logging.IMP.error("[Redirect Lobby] Failed execute runnable! Task {} | Details: {}",
                                unit, any);
                    } finally {
                        waitSend.remove(unit.gamer);
                    }
                }
            }
        }

        @Override
        protected void handleCancel(@NotNull final CancelReason reason) {
            taskRun.set(false);
            waitSend.clear();
        }

        @ToString(
                exclude = {"runnable"}
        )
        @EqualsAndHashCode(
                exclude = {"delay", "runnable"}
        )
        @AllArgsConstructor
        @FieldDefaults(level = AccessLevel.PRIVATE)
        private static class TaskUnit {
            final Gamer gamer;
            Runnable runnable;
            int delay;

            public boolean isDelay() {
                --delay;
                return delay <= 0;
            }
        }
    }
}
