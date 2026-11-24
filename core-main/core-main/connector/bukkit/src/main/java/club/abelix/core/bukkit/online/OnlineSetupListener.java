package club.abelix.core.bukkit.online;

import club.abelix.core.bukkit.event.ConnectionEstablishedEvent;
import club.abelix.core.bukkit.updater.SimpleServerStatusUpdater;
import club.abelix.di.platform.bukkit.DiBukkitPlatform;
import club.abelix.network.online.info.ServerInfo;
import club.abelix.network.online.service.StatusTrackingService;
import club.abelix.network.online.updater.ServerStatusUpdater;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class OnlineSetupListener implements Listener {

    ServerInfo serverInfo;
    StatusTrackingService trackingService;

    @EventHandler
    public void onConnected(
            final @NotNull ConnectionEstablishedEvent event
    ) {
        DiBukkitPlatform.withBeanOrElse(
                ServerStatusUpdater.class,
                updater -> trackingService.track(serverInfo, updater),
                () -> trackingService.track(serverInfo, new SimpleServerStatusUpdater())
        );
    }

}
