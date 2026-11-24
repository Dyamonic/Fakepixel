package club.abelix.metrics.listener;

import club.abelix.core.app.api.plugin.Listener;
import club.abelix.core.app.event.gamer.PlayerQuitEvent;
import club.abelix.metrics.task.PlayerLeaveTask;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: agera-core</p>
 * <p>PlayerLeaveListener creation on 20.06.2024 at 16:00</p>
 */

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class PlayerLeaveListener implements Listener {
    PlayerLeaveTask task;

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onPlayerJoin(final PlayerQuitEvent event) {
        task.getLeaves().incrementAndGet();
    }
}
