package club.abelix.core.app.data.queue.waiting;

import club.abelix.core.app.CoreApplication;
import club.abelix.core.app.api.plugin.Listener;
import club.abelix.core.app.connection.server.AbstractConnectable;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.event.gamer.PlayerQuitEvent;
import club.abelix.core.app.event.gamer.PlayerRedirectEvent;
import club.abelix.network.type.mode.Mini;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.EnumMap;

/**
 * @author WhileInside
 */
public final class GameQueueManager implements Listener {

    public static final GameQueueManager INSTANCE = new GameQueueManager();
    private final EnumMap<Mini, GameQueue> gameQueues = new EnumMap<>(Mini.class);

    private GameQueueManager() {
        CoreApplication.getInstance().getPluginManager().registerListener(this);
    }

    public boolean quitQueue(Gamer gamer) {
        boolean removed = false;

        synchronized (gameQueues) {
            for (GameQueue queue : gameQueues.values()) {
                removed = queue.removeUser(gamer);
            }
        }

        return removed;
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onPlayerRedirect(PlayerRedirectEvent event) {
        quitQueue(event.getGamer());
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onUserQuit(PlayerQuitEvent event) {
        quitQueue(event.getGamer());
    }

    public void register(AbstractConnectable server, Mini mini, int players) {
        synchronized (gameQueues) {
            GameQueue queue = gameQueues.computeIfAbsent(mini, v -> new GameQueue(v, players));
            queue.addRegistrar(server);
        }
    }

    public GameQueue getQueue(Mini mini) {
        synchronized (gameQueues) {
            return gameQueues.get(mini);
        }
    }

    public void unregister(AbstractConnectable server) {
        synchronized (gameQueues) {
            gameQueues.values().removeIf(queue -> {
                queue.removeRegistrar(server);

                return !queue.hasRegistrars();
            });
        }
    }

}
