package club.abelix.network.online.query.execution.standard;

import club.abelix.network.online.query.StatusQuery;
import club.abelix.network.online.query.StatusQueryParams;
import club.abelix.network.online.query.StatusQueryResult;
import club.abelix.network.online.query.execution.StatusQueryExecutionService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public final class StandardCachedStatusQueryExecutionService implements StatusQueryExecutionService {

    StatusQueryExecutionService delegate;

    Cache<StatusQueryParams, StatusQueryResult> cache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .maximumSize(100000)
            .build();

    AtomicBoolean shutdown = new AtomicBoolean(false);

    @Override
    public @NotNull StatusQueryResult executeSync(@NotNull StatusQuery query, boolean refresh) {
        if (shutdown.get()) {
            log.warn("Cached status query execution attempted during shutdown");
            return StatusQueryResult.EMPTY;
        }

        StatusQueryParams params = query.params();

        if (refresh) {
            // принудительно пробиваем Redis и обновляем кеш
            StatusQueryResult fresh = delegate.executeSync(query, true);
            cache.put(params, fresh);
            return markCached(fresh, false);
        }

        StatusQueryResult result = cache.get(params, p -> delegate.executeSync(query, true));

        return markCached(result, true);
    }

    @Override
    public @NotNull CompletableFuture<StatusQueryResult> executeAsync(
            @NotNull StatusQuery query,
            boolean refresh
    ) {
        return CompletableFuture.supplyAsync(
                () -> executeSync(query, refresh)
        );
    }

    @Override
    public void shutdown() {
        if (shutdown.compareAndSet(false, true)) {
            delegate.shutdown();
            cache.invalidateAll();
        }
    }

    private StatusQueryResult markCached(StatusQueryResult original, boolean cached) {
        return new StatusQueryResult(
                original.getTotalOnline(),
                original.getLookedUpServers(),
                original.getPerServer(),
                cached
        );
    }
}
