package club.abelix.commons.util.loading;

import club.abelix.commons.util.loading.caching.CachingManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Менеджер по управлению загрузками данных
 *
 * @param <K> Ключ, по которому будет загрузка данных
 * @param <V> Тип данных
 */
public interface LoadingManager<K, V> {

    @NotNull CompletableFuture<V> getOrLoad(@NotNull K key);

    @Nullable CachingManager<K, V> getCache();

}
