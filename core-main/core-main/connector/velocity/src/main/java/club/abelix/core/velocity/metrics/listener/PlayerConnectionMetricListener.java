package club.abelix.core.velocity.metrics.listener;

import club.abelix.core.velocity.metrics.VelocityMetricsStorage;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PlayerConnectionMetricListener {

    VelocityMetricsStorage storage;

    @Subscribe(priority = Short.MAX_VALUE)
    public void onPreLogin(PreLoginEvent event) {
        storage.getLoginEvent().incrementAndGet();
    }

    @Subscribe(priority = Short.MAX_VALUE)
    public void onServerConnect(ServerPreConnectEvent event) {
        storage.getServerMoveEvent().incrementAndGet();
    }
}
