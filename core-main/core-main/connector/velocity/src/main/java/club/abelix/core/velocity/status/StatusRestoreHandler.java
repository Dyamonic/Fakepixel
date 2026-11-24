package club.abelix.core.velocity.status;

import club.abelix.network.online.info.ServerInfo;
import club.abelix.network.online.service.StatusTrackingService;
import club.abelix.network.redis.RedisConnectionObserver;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public final class StatusRestoreHandler implements RedisConnectionObserver {

    ServerInfo serverInfo;
    StatusTrackingService statusTrackingService;

    @Override
    public void onConnected() {
        log.info("Connection to Redis restored. Resuming status tracking.");
        statusTrackingService.restore(
                serverInfo
        );
    }

    @Override
    public void onDisconnected() {
        log.error("Lost connection to Redis. Status tracking will be paused until connection is restored.");
    }
}
