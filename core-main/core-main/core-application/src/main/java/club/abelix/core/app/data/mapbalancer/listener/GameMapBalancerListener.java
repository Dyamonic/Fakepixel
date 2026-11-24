package club.abelix.core.app.data.mapbalancer.listener;

import club.abelix.core.app.api.plugin.Listener;
import club.abelix.core.app.connection.server.ArenaServer;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.mapbalancer.GameMapBalancerManager;
import club.abelix.core.app.event.server.ServerDisconnectEvent;
import club.abelix.network.type.mode.Mini;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>GameMapBalancerListener creation on 17.01.2024 at 12:39</p>
 */

public class GameMapBalancerListener implements Listener {

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onServerDisconnect(ServerDisconnectEvent serverDisconnectEvent) {
        if (!(serverDisconnectEvent.getServer() instanceof ArenaServer arenaServer)) return;

        Mini mini = arenaServer.getMini();

        if (ServerManager.IMP.getServers(mini).isEmpty()) {
            GameMapBalancerManager.IMP.removeBalancer(mini);
        }

    }
}
