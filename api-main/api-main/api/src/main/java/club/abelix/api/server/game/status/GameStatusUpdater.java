package club.abelix.api.server.game.status;

import club.abelix.api.server.game.Game;
import club.abelix.network.online.info.ArenaServerInfo;
import club.abelix.network.online.info.ServerInfo;
import club.abelix.network.online.updater.ServerStatusUpdater;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class GameStatusUpdater implements ServerStatusUpdater {

    Game game;

    @Override
    public ServerInfo update(ServerInfo serverInfo) {
        if (serverInfo instanceof ArenaServerInfo arenaServerInfo) {
            return ArenaServerInfo.ofArena(
                    arenaServerInfo,
                    game.getNotVanishedOnline(),
                    game.getGamers().size() - game.getNotVanishedOnline(),
                    game.getGameState().name(),
                    game.getGameMap().getTechnicalName()
            );
        }

        throw new IllegalStateException("Unsupported server info type: " + serverInfo.getClass().getName());
    }

}
