package club.abelix.metrics.listener;

import club.abelix.core.app.api.plugin.Listener;
import club.abelix.core.app.event.gamer.PlayerRedirectEvent;
import club.abelix.metrics.task.PlayerMoveTask;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>ServerMovingListener creation on 20.02.2024 at 00:08</p>
 */
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class ServerMovingListener implements Listener {

    PlayerMoveTask moveTask;

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onGamerMode(final PlayerRedirectEvent event) {
        moveTask.getMoves().getAndIncrement();
    }
}
