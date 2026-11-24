package club.abelix.core.app.api.request;

import club.abelix.commons.util.request.RequestManager;
import club.abelix.core.app.CoreApplication;
import club.abelix.core.app.api.plugin.Listener;
import club.abelix.core.app.event.gamer.PlayerQuitEvent;
import lombok.NonNull;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * @author artem
 */
public class RequestManagerImpl extends RequestManager implements Listener {

    public RequestManagerImpl(final CoreApplication core) {
        core.getPluginManager().registerListener(this);
    }

    public static void injectManager(final @NonNull CoreApplication core) {
        setImplementation(new RequestManagerImpl(core));
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onQuit(final PlayerQuitEvent event) {
        handleQuit(event.getGamer().getPlayerId());
    }

}
