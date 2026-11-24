package club.abelix.commons.util.loading.caching;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author lero4ka16
 */
public interface CachingManager<K, V> {

    void invalidate(@NotNull K key);

    void invalidate();

    void put(@NotNull K key, @NotNull V value);

    @Nullable V get(@NotNull K key);

}
