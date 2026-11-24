package club.abelix.network.online.service;

import club.abelix.network.online.info.ServerInfo;
import club.abelix.network.online.updater.ServerStatusUpdater;
import org.jetbrains.annotations.NotNull;

public interface StatusTrackingService {

    void track(
            @NotNull ServerInfo serverInfo,
            @NotNull ServerStatusUpdater updater
    );

    void restore(
            @NotNull ServerInfo serverInfo
    );

    void restoreAll();

    void update(@NotNull ServerInfo serverInfo);

    void stop(@NotNull String id);

    void shutdown();

}
