package club.abelix.network.online.updater;

import club.abelix.network.online.info.ServerInfo;

public interface ServerStatusUpdater {

    ServerInfo update(ServerInfo serverInfo);

}
