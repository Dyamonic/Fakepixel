package club.abelix.commons.util.loading.caching.guava.strategy;

import com.google.common.cache.CacheBuilder;
import lombok.Data;

import java.util.concurrent.TimeUnit;

/**
 * @author lero4ka16
 */
@Data
public final class ExpireAfterWrite implements GuavaCachingStrategy {

    private final long time;
    private final TimeUnit unit;

    public static ExpireAfterWrite get(long time, TimeUnit unit) {
        return new ExpireAfterWrite(time, unit);
    }

    @Override
    public void apply(CacheBuilder<Object, Object> builder) {
        builder.expireAfterWrite(time, unit);
    }

}
