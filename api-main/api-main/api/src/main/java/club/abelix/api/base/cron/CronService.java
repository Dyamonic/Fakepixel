package club.abelix.api.base.cron;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.network.redis.JedisConnector;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import redis.clients.jedis.Jedis;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CronService {
    int REDIS_DB = 2;

    JedisConnector jedisConnector;

    public CronService(JedisConnector jedisConnector) {
        this.jedisConnector = jedisConnector;
    }

    public CronService(Object corePlugin) {
        this(resolveJedisConnector(corePlugin));
    }

    public CompletableFuture<Boolean> check(Gamer gamer, String identifier, Duration duration) {
        val redisKey = String.format("cron/%s/%s", gamer.getName().toLowerCase(), identifier);

        return CompletableFuture.supplyAsync(() -> {
            try (Jedis jedis = jedisConnector.withJedis()) {
                jedis.select(REDIS_DB);

                String data = jedis.get(redisKey);

                if (data == null || data.isEmpty()) {
                    jedis.setex(redisKey, duration.toSeconds(), "true");
                    return true;
                }

                return false;
            }
        }).handle((result, exception) -> {
            if (exception != null) {
                log.error("Failed cron service gamer: {} ident: {}. Details: ", gamer, identifier, exception);
                throw new RuntimeException(exception);
            }
            return result;
        });
    }

    public CompletableFuture<Boolean> check(Gamer gamer, String identifier) {
        return check(gamer, identifier, Duration.ofDays(1));
    }

    private static JedisConnector resolveJedisConnector(Object corePlugin) {
        if (corePlugin == null) {
            throw new IllegalStateException("Core plugin is not available");
        }

        if (corePlugin instanceof JedisConnector) {
            return (JedisConnector) corePlugin;
        }

        for (String methodName : new String[]{"getJedisConnector", "getRedisConnector"}) {
            try {
                Method method = corePlugin.getClass().getMethod(methodName);
                Object connector = method.invoke(corePlugin);

                if (connector instanceof JedisConnector) {
                    return (JedisConnector) connector;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }

        for (String fieldName : new String[]{"jedisConnector", "redisConnector"}) {
            try {
                Field field = corePlugin.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                Object connector = field.get(corePlugin);

                if (connector instanceof JedisConnector) {
                    return (JedisConnector) connector;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }

        throw new IllegalStateException("Failed to resolve JedisConnector from " + corePlugin.getClass().getName());
    }
}
