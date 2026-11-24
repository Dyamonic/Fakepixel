package club.abelix.metrics.listener;

import club.abelix.core.app.api.plugin.Listener;
import club.abelix.core.app.event.gamer.PlayerLoginEvent;
import club.abelix.metrics.task.PlayerJoinsTask;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>PlayerJoinsMetricListener creation on 19.02.2024 at 23:38</p>
 */

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class PlayerJoinsMetricListener implements Listener {
    PlayerJoinsTask task;

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onPlayerJoin(final PlayerLoginEvent event) {
        task.getJoins().incrementAndGet();
    }
}
