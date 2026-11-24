package club.abelix.core.velocity.task;

import club.abelix.network.session.PlayerRedisSessionService;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.kyori.adventure.text.Component;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class PlayerRedisSessionRefreshTask implements Runnable {

    ProxyServer proxyServer;
    PlayerRedisSessionService sessionService;

    @Override
    public void run() {
        val players = proxyServer.getAllPlayers();

        if (players.isEmpty()) return;

        int initialSize = players.size();
        int successRefresh = 0;
        int failedRefresh = 0;

        log.info("Session refresh task running for {} players", players.size());

        for (val player : players) {
            val playerName = player.getUsername();
            val refresh = sessionService.refresh(playerName);

            if (!refresh) {
                log.warn("Failed to refresh session for {}", player);
                failedRefresh++;

                player.disconnect(Component.text("Failed to refresh your session"));
                players.remove(player);

                continue;
            }

            successRefresh++;
        }

        log.info(
                "Session refresh completed. Result: Success {} Failed {}, Players: Before refresh {} After refresh {}",
                successRefresh,
                failedRefresh,
                initialSize,
                proxyServer.getAllPlayers().size()
        );
    }
}
