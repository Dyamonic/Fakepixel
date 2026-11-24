package club.abelix.core.bukkit.updater;

import club.abelix.network.online.info.ServerInfo;
import club.abelix.network.online.info.StandardServerInfo;
import club.abelix.network.online.updater.ServerStatusUpdater;
import org.bukkit.Bukkit;

public class SimpleServerStatusUpdater implements ServerStatusUpdater {

    @Override
    public ServerInfo update(ServerInfo serverInfo) {
        return new StandardServerInfo(
                serverInfo.getId(),
                serverInfo.getServerName(),
                serverInfo.getServerMode(),
                serverInfo.getKind(),
                serverInfo.getTags(),
                100,
                Bukkit.getOnlinePlayers().size(),
                0
        );
    }

}
