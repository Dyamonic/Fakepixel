package club.abelix.core.velocity.status;

import club.abelix.core.velocity.gamer.Gamer;
import club.abelix.network.online.info.ServerInfo;
import club.abelix.network.online.info.StandardServerInfo;
import club.abelix.network.online.updater.ServerStatusUpdater;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class VelocityStatusUpdater implements ServerStatusUpdater {

    @Override
    public ServerInfo update(ServerInfo serverInfo) {
        return new StandardServerInfo(
                serverInfo.getId(),
                serverInfo.getServerName(),
                serverInfo.getServerMode(),
                serverInfo.getKind(),
                serverInfo.getTags(),
                100,
                Gamer.getNonVanishedGamers().size(),
                Gamer.getGamers().size() - Gamer.getNonVanishedGamers().size()
        );
    }
}
