package club.abelix.commons.util.loading.caching.guava.strategy;

import com.google.common.cache.CacheBuilder;

/**
 * @author lero4ka16
 */
public interface GuavaCachingStrategy {

    static GuavaCachingStrategy combine(GuavaCachingStrategy... strategies) {
        GuavaCachingStrategy result = null;

        for (GuavaCachingStrategy strategy : strategies) {
            result = result == null ? strategy : result.with(strategy);
        }

        return result;
    }

    void apply(CacheBuilder<Object, Object> builder);

    default GuavaCachingStrategy with(GuavaCachingStrategy another) {
        return builder -> {
            apply(builder);
            another.apply(builder);
        };
    }

}
