package club.abelix.commons.util.loading.caching.guava;

import club.abelix.commons.util.loading.caching.CachingManager;
import club.abelix.commons.util.loading.caching.guava.strategy.GuavaCachingStrategy;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author lero4ka16
 */
public final class GuavaCachingManager<K, V> implements CachingManager<K, V> {

    private final Cache<K, V> cache;

    public GuavaCachingManager(GuavaCachingStrategy strategy) {
        CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
        strategy.apply(builder);

        cache = builder.build();
    }

    @Override
    public void invalidate(@NotNull K key) {
        cache.invalidate(key);
    }

    @Override
    public void invalidate() {
        cache.invalidateAll();
    }

    @Override
    public void put(@NotNull K key, @NotNull V value) {
        this.cache.put(key, value);
    }

    @Override
    public @Nullable V get(@NotNull K key) {
        return cache.getIfPresent(key);
    }

}
