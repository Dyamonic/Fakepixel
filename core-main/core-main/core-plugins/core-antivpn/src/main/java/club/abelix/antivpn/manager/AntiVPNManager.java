package club.abelix.antivpn.manager;

import club.abelix.antivpn.data.CacheEntry;
import club.abelix.antivpn.data.ProxyCheckResult;
import club.abelix.antivpn.sql.ASNWhitelistSqlHandler;
import club.abelix.antivpn.sql.ProxyCheckSqlHandler;
import club.abelix.commons.geo.proxy.GeoProxyService;
import club.abelix.network.common.logging.Logging;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */

@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class AntiVPNManager {

    Cache<String, CacheEntry> checkCache = Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.HOURS)
            .build();

    @Getter
    List<String> asnWhitelist = new ArrayList<>();

    GeoProxyService geoProxyService;

    public AntiVPNManager(@NonNull String serviceUrl) {
        geoProxyService = new GeoProxyService(serviceUrl);
    }

    public void loadWhitelist() {
        if (!asnWhitelist.isEmpty()) {
            asnWhitelist.clear();
        }

        ASNWhitelistSqlHandler.INSTANCE.getAll().thenAccept(results -> {
            asnWhitelist.addAll(results.stream().map(result -> result.asn().toLowerCase()).toList());
            Logging.IMP.info("Loaded " + asnWhitelist.size() + " whitelisted ASNs");
        }).join();
    }

    public CompletableFuture<Boolean> check(@NonNull String ip) {
        val cacheResult = checkCache.getIfPresent(ip);
        if (cacheResult != null) {
            if (asnWhitelist.contains(cacheResult.asn().toLowerCase())) {
                checkCache.put(ip, new CacheEntry(false, cacheResult.banned(), cacheResult.asn().toLowerCase()));
                return CompletableFuture.completedFuture(false);
            } else if (cacheResult.banned()) {
                checkCache.put(ip, new CacheEntry(true, true, cacheResult.asn().toLowerCase()));
                return CompletableFuture.completedFuture(true);
            }

            return CompletableFuture.completedFuture(cacheResult.verdict());
        }

        return ProxyCheckSqlHandler.INSTANCE.check(ip).thenCompose(result -> {
            if (result != null) {
                if (result.added().isAfter(LocalDateTime.now().minusMonths(1))) {
                    checkCache.put(ip, new CacheEntry(result.verdict(), result.verdict(), result.asn().toLowerCase()));
                    return CompletableFuture.completedFuture(result.verdict());
                }

                ProxyCheckSqlHandler.INSTANCE.delete(ip);
            }

            return geoProxyService.check(ip).thenApply(response -> {
                int risk = response.risk();

                boolean isProxy = response.proxy().equals("yes") && risk > 50;
                boolean verdict = isProxy;

                val ans = response.asn() != null ? response.asn() : "unknown";
                if (asnWhitelist.contains(ans.toLowerCase())) {
                    verdict = false;
                }

                ProxyCheckSqlHandler.INSTANCE.insert(new ProxyCheckResult(ip, isProxy, LocalDateTime.now(), response.asn().toLowerCase()));
                checkCache.put(ip, new CacheEntry(verdict, isProxy, ans.toLowerCase()));

                return verdict;
            });
        });
    }

}
