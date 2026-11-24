package club.abelix.commons.util.loading.caching.guava.strategy;

import com.google.common.cache.CacheBuilder;
import lombok.Data;

import java.util.concurrent.TimeUnit;

/**
 * @author lero4ka16
 */
@Data
public final class ExpireAfterAccess implements GuavaCachingStrategy {

    private final long time;
    private final TimeUnit unit;

    public static ExpireAfterAccess get(long time, TimeUnit unit) {
        return new ExpireAfterAccess(time, unit);
    }

    @Override
    public void apply(CacheBuilder<Object, Object> builder) {
        builder.expireAfterAccess(time, unit);
    }

}
