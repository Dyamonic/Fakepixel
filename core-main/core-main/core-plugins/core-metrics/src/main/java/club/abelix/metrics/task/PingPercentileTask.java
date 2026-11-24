package club.abelix.metrics.task;

import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.commons.metric.base.MetricGaugeTask;
import club.abelix.network.packet.play.client.CPlayerPingResponse;
import club.abelix.network.packet.play.server.SPlayerPingRequest;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public final class PingPercentileTask extends MetricGaugeTask {
    private static final ServerManager SERVER_MANAGER = ServerManager.IMP;


    public PingPercentileTask() {
        super("player_ping", "player ping", 15, TimeUnit.SECONDS,
                "proxy_count", "median", "p95", "trimmed_mean");
    }


    @Override
    protected void update() {
        val futureList = Sets.<CompletableFuture<CPlayerPingResponse>>newHashSet();

        SERVER_MANAGER.getProxies().forEach(proxy ->
                futureList.add(proxy.getConnection().awaitPacket(new SPlayerPingRequest())));

        CompletableFuture.allOf(futureList.toArray(CompletableFuture[]::new)).thenAccept(unused -> {
            val results = new ArrayList<ProxyResult>();

            for (val f : futureList) {
                assert f != null;

                try {
                    f.thenAccept(cPlayerPingResponse -> results.add(
                        new ProxyResult(cPlayerPingResponse.getMedian(),
                        cPlayerPingResponse.getP95(),
                        cPlayerPingResponse.getTrimmedMean()))).exceptionally(throwable -> {
                        log.error("Failed player ping result packet ", throwable);
                        return null;
                    }).get(5, TimeUnit.SECONDS);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    log.error("Fail to get proxy player ping results", e);
                }
            }

            if (results.isEmpty()) return;
            val proxyProcessedCount = results.size();

            double medianCount = 0;
            double p95Count = 0;
            double trimmedMeanCount = 0;

            for (val result : results) {
                medianCount += result.median;
                p95Count += result.trimmedMean;
                trimmedMeanCount += result.trimmedMean;
            }

            getCollector().labels("proxy_count").set(proxyProcessedCount);
            getCollector().labels("median_count").set(medianCount/proxyProcessedCount);
            getCollector().labels("p95_count").set(p95Count/proxyProcessedCount);
            getCollector().labels("trimmed_mean_count").set(trimmedMeanCount);
        });

        super.update();
    }

    @Override
    protected Double getValue() {
        return null;
    }

    private record ProxyResult(double median,
                               double p95,
                               double trimmedMean) {}
}
