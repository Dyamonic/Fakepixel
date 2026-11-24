package club.abelix.network.session;

import club.abelix.network.redis.JedisConnector;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.params.SetParams;

import java.util.Optional;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class PlayerRedisSessionService {

    private static final long TTL_SECONDS = 5 * 60;

    JedisConnector connector;

    static final int REDIS_DB_INDEX = 0;

    @NotNull
    public Optional<@NotNull PlayerSession> start(
            final @NotNull String playerName,
            final @NotNull String proxyName
    ) {
        val key = formatKey(playerName);
        val guardKey = key + ":guard";

        try (val jedis = connector.withJedis()) {
            jedis.select(REDIS_DB_INDEX);

            val ok = jedis.set(guardKey, "1", SetParams.setParams().nx().ex(TTL_SECONDS));

            if (ok == null) {
                return Optional.empty();
            }

            val session = new PlayerSession(
                    playerName,
                    proxyName,
                    System.currentTimeMillis()
            );

            val pipe = jedis.pipelined();
            pipe.hset(key, session.asHash());
            pipe.expire(key, TTL_SECONDS);
            pipe.sync();

            return Optional.of(session);
        }
    }

    public PlayerSession getSession(final @NotNull String playerName) {
        val key = formatKey(playerName);

        try (val jedis = connector.withJedis()) {
            jedis.select(REDIS_DB_INDEX);

            if (jedis.exists(key)) {
                val sessionData = jedis.hgetAll(key);
                if (sessionData == null || sessionData.isEmpty()) {
                    return null;
                }

                return PlayerSession.fromHash(sessionData);
            }

            return null;
        }
    }

    public boolean isSessionOpened(final @NotNull String playerName) {
        val key = formatKey(playerName);

        try (val jedis = connector.withJedis()) {
            jedis.select(REDIS_DB_INDEX);
            return jedis.exists(key);
        }
    }

    public boolean closeSession(final @NotNull String playerName) {
        val key = formatKey(playerName);
        val guardKey = key + ":guard";

        try (val jedis = connector.withJedis()) {
            jedis.select(REDIS_DB_INDEX);
            val deleted = jedis.del(key, guardKey);
            return deleted > 0;
        }
    }

    public boolean refresh(final @NotNull String playerName) {
        val key = formatKey(playerName);
        val guardKey = key + ":guard";

        try (val jedis = connector.withJedis()) {
            jedis.select(REDIS_DB_INDEX);

            if (!jedis.exists(key)) {
                return false;
            }

            val newLastUpdate = System.currentTimeMillis();
            val pipe = jedis.pipelined();
            pipe.hset(key, "lastUpdate", String.valueOf(newLastUpdate));
            pipe.expire(key, TTL_SECONDS);
            pipe.expire(guardKey, TTL_SECONDS);
            pipe.sync();

            return true;
        }
    }

    public String formatKey(String playerName) {
        return ("player_session:" + playerName).toLowerCase();
    }
}
