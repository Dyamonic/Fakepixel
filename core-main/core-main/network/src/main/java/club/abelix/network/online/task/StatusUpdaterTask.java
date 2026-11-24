package club.abelix.network.online.task;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import club.abelix.network.online.info.ServerInfo;
import club.abelix.network.online.service.StatusTrackingService;
import club.abelix.network.online.updater.ServerStatusUpdater;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class StatusUpdaterTask implements Runnable {

    StatusTrackingService statusTrackingService;
    ServerStatusUpdater statusUpdater;
    ServerInfo serverInfo;

    @Override
    public void run() {
        val newStatus = statusUpdater.update(serverInfo);

        if (newStatus.equals(serverInfo)) {
            log.debug("No status change detected for server {}", serverInfo.getId());
            return;
        }

        statusTrackingService.update(newStatus);
    }

}
