package club.abelix.network.online.updater.noop;

import club.abelix.network.online.info.ServerInfo;
import club.abelix.network.online.info.StandardServerInfo;
import club.abelix.network.online.updater.ServerStatusUpdater;

import java.util.concurrent.ThreadLocalRandom;

public class NoopServerStatusUpdater implements ServerStatusUpdater {

    @Override
    public ServerInfo update(ServerInfo serverInfo) {
        return new StandardServerInfo(
                serverInfo.getId(),
                serverInfo.getServerName(),
                serverInfo.getServerMode(),
                serverInfo.getKind(),
                serverInfo.getTags(),
                0,
                0,
                0
        );
    }

}
