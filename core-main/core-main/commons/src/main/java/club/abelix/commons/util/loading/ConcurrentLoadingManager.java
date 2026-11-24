package club.abelix.commons.util.loading;

import club.abelix.commons.util.loading.caching.CachingManager;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class ConcurrentLoadingManager<K, V> implements LoadingManager<K, V> {

    private final Map<K, CompletableFuture<V>> futures;

    @Getter
    private final CachingManager<K, V> cache;

    private final Function<K, CompletableFuture<V>> loader;

    public ConcurrentLoadingManager(CachingManager<K, V> cache,
                                    Function<K, CompletableFuture<V>> loader) {
        this.futures = new ConcurrentHashMap<>();

        this.cache = cache;
        this.loader = loader;
    }

    public ConcurrentLoadingManager(Function<K, CompletableFuture<V>> loader) {
        this(null, loader);
    }

    @Override
    public @NotNull CompletableFuture<V> getOrLoad(@NotNull K key) {
        CompletableFuture<V> exist = futures.get(key);

        if (exist != null) {
            return exist;
        }

        if (cache != null) {
            V cached = cache.get(key);

            if (cached != null) {
                return CompletableFuture.completedFuture(cached);
            }
        }

        CompletableFuture<V> future = loader.apply(key);
        futures.put(key, future);

        return future.thenApply(result -> {
            futures.remove(key, future);

            if (cache != null) {
                cache.put(key, result);
            }

            return result;
        });
    }


}
