package club.abelix.commons.util.loading.caching.guava.strategy;

import com.google.common.cache.CacheBuilder;
import lombok.Data;

/**
 * @author lero4ka16
 */
@Data
public final class SoftValues implements GuavaCachingStrategy {

    private static final SoftValues INSTANCE = new SoftValues();

    public static SoftValues get() {
        return INSTANCE;
    }

    @Override
    public void apply(CacheBuilder<Object, Object> builder) {
        builder.softValues();
    }

}
