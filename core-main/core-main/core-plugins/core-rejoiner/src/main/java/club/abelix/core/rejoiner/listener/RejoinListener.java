package club.abelix.core.rejoiner.listener;

import club.abelix.core.app.api.plugin.Listener;
import club.abelix.core.app.connection.server.AbstractServer;
import club.abelix.core.app.connection.server.ArenaServer;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.event.arena.ArenaChangeStateEvent;
import club.abelix.core.app.event.gamer.GamerJoinEvent;
import club.abelix.core.rejoiner.CoreRejoinerCorePlugin;
import club.abelix.network.common.enums.GameState;
import lombok.RequiredArgsConstructor;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

@RequiredArgsConstructor
public class RejoinListener implements Listener {

    private final CoreRejoinerCorePlugin plugin;

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onPlayerJoin(GamerJoinEvent event) {
        Gamer player = event.getGamer();

        AbstractServer server = plugin.getRejoinManager().getRejoinableServer(player);

        if (server != null) {
            player.sendLocalizedMessage("rejoin_available", "%server%", server.getName());
        }
    }

    @Subscribe
    public void onCorePush(ArenaChangeStateEvent event) {
        ArenaServer arena = event.getServer();

        //на эту игру нельзя перезаходить
        if (!plugin.getRejoinManager().canRejoin(arena.getMaster())) {
            return;
        }

        GameState newState = event.getNewState();

        //игра началась - всем игрокам сервера ставим rejoin-сервер
        if (newState == GameState.GAME) {
            //устанавливаем всем сервер для реджоина
            for (Gamer player : arena.getGamers()) {
                plugin.getRejoinManager().setRejoinableServer(player, arena);
            }
        } else if (newState == GameState.STARTING || newState == GameState.ENDING) {
            //игра закончилась - удаляем всем реджоин-сервер
            plugin.getRejoinManager().clearRejoinableGamers(arena);
        }
    }

}
