package club.abelix.core.velocity.metrics.listener;

import club.abelix.core.velocity.metrics.VelocityMetricsStorage;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PingMetricListener {

    VelocityMetricsStorage storage;

    @Subscribe(priority = Short.MAX_VALUE)
    public void onPingEvent(ProxyPingEvent event) {
        storage.getPingEvent().incrementAndGet();
    }
}
