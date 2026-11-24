package club.abelix.api.base.online;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.di.api.annotation.Bean;
import club.abelix.network.online.info.ServerInfo;
import club.abelix.network.online.info.StandardServerInfo;
import club.abelix.network.online.updater.ServerStatusUpdater;
import org.bukkit.Bukkit;

@Bean
public class StandardOnlineInfoUpdater implements ServerStatusUpdater {

    @Override
    public ServerInfo update(ServerInfo serverInfo) {
        return new StandardServerInfo(
                serverInfo.getId(),
                serverInfo.getServerName(),
                serverInfo.getServerMode(),
                serverInfo.getKind(),
                serverInfo.getTags(),
                Bukkit.getMaxPlayers(),
                Gamer.getNonVanishedGamers().size(),
                Gamer.getGamers().size() - Gamer.getNonVanishedGamers().size()
        );
    }
}
