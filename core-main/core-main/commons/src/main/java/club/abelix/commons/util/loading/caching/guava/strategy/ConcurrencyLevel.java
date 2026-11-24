package club.abelix.commons.util.loading.caching.guava.strategy;

import com.google.common.cache.CacheBuilder;
import lombok.Data;

/**
 * @author lero4ka16
 */
@Data
public final class ConcurrencyLevel implements GuavaCachingStrategy {

    private final int concurrencyLevel;

    public static ConcurrencyLevel get(int concurrencyLevel) {
        return new ConcurrencyLevel(concurrencyLevel);
    }

    @Override
    public void apply(CacheBuilder<Object, Object> builder) {
        builder.concurrencyLevel(concurrencyLevel);
    }

}
