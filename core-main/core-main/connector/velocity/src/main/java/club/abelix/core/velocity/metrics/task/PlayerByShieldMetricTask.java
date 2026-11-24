package club.abelix.core.velocity.metrics.task;

import club.abelix.commons.metric.base.MetricGaugeTask;
import club.abelix.commons.geo.proxy.GeoProxyService;
import club.abelix.core.velocity.gamer.Gamer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import lombok.val;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class PlayerByShieldMetricTask extends MetricGaugeTask {

    private final GeoProxyService geoProxyService;

    public PlayerByShieldMetricTask(String geoService) {
        super("player_by_shield",
                "Player by shield",
                30, TimeUnit.SECONDS,
                "asn");
        this.geoProxyService = new GeoProxyService(geoService);
    }

    @Override
    protected void update() {
        val ipCountMap = Gamer.getGamers().stream().map(gamer -> {
            val connectedPlayer = (ConnectedPlayer) gamer.getPlayer();
            return ((InetSocketAddress) connectedPlayer.getConnection().getChannel().remoteAddress())
                    .getAddress().getHostAddress();
        }).collect(Collectors.groupingBy(ip -> ip, Collectors.counting()));

        val asnCountMap = new HashMap<String, Long>();

        val futures = ipCountMap.entrySet().stream().map(entry -> {
            val ip = entry.getKey();
            val count = entry.getValue();
            return geoProxyService.check(ip).thenAccept(response -> {
                val asn = response.asn() != null ? response.asn() : "unknown";
                synchronized (asnCountMap) {
                    asnCountMap.merge(asn, count, Long::sum);
                }
            }).exceptionally(ex -> null);
        }).toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures).join();

        asnCountMap.forEach((asn, count) -> getCollector().labels(asn).set(count));

    }

    @Override
    protected Double getValue() {
        return null;
    }
}
