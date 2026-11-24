package club.abelix.network.redis;

import club.abelix.network.metrics.RedisPerformanceMetrics;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import w.util.Env;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class JedisConnector {

    private volatile JedisPool pool;
    private final List<RedisConnectionObserver> observers = new CopyOnWriteArrayList<>();
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean heartbeatStarted = new AtomicBoolean(false);
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder()
                    .setNameFormat("redis-heartbeat-%d")
                    .setDaemon(true)
                    .build()
    );

    private static final int MAX_RECONNECT_ATTEMPTS = 10;
    private static final int RECONNECT_INTERVAL_MS = 5000;

    public void addObserver(RedisConnectionObserver observer) {
        observers.add(observer);
        if (connected.get()) {
            observer.onConnected();
        }
    }

    public void removeObserver(RedisConnectionObserver observer) {
        observers.remove(observer);
    }

    public boolean isConnected() {
        return connected.get();
    }

    public synchronized void establish() {
        if (pool != null && !pool.isClosed()) {
            try (val jedis = pool.getResource()) {
                val pingText = "HELLO";
                if (pingText.equalsIgnoreCase(jedis.ping(pingText))) {
                    if (connected.compareAndSet(false, true)) {
                        log.info("Redis connection verified and restored");
                        observers.forEach(RedisConnectionObserver::onConnected);
                    }
                    return;
                }
            } catch (Exception e) {
                log.warn("Jedis pool is established but health check failed: {}. Re-initializing...", e.getMessage());
            }

            try {
                pool.close();
            } catch (Exception e) {
                log.error("Failed to close broken Jedis pool", e);
            }
        }

        val host = Env.getString("REDIS_HOST");
        val port = Env.getInt("REDIS_PORT", 6379);
        val password = Env.getString("REDIS_PASSWORD");
        val user = Env.getString("REDIS_USER");

        val maxPoolSize = Env.getInt("REDIS_MAX_POOL", 128);
        val minIdle = Env.getInt("REDIS_MIN_IDLE", 1);

        if (host.isBlank()) {
            throw new IllegalStateException("REDIS_HOST is not configured");
        }

        var clientConfig = DefaultJedisClientConfig.builder()
                .user(user)
                .password(password)
                .connectionTimeoutMillis(Env.getInt("REDIS_CONNECT_TIMEOUT", 1000))
                .socketTimeoutMillis(Env.getInt("REDIS_SOCKET_TIMEOUT", 1000))
                .build();

        val poolConfig = new GenericObjectPoolConfig<Jedis>();

        poolConfig.setMaxTotal(maxPoolSize);
        poolConfig.setMaxIdle(maxPoolSize);
        poolConfig.setMinIdle(minIdle);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(1));

        JedisPool newPool;
        for (int attempt = 0; attempt < MAX_RECONNECT_ATTEMPTS; attempt++) {
            try {
                newPool = new JedisPool(poolConfig, new HostAndPort(host, port), clientConfig);

                log.info("Sending ping to redis at {}:{} ...", host, port);

                try (val jedis = newPool.getResource()) {
                    val pingText = "HELLO";
                    val res = jedis.ping(pingText);

                    if (!pingText.equalsIgnoreCase(res)) {
                        log.error("Received incorrect ping text from redis: {}", res);
                        throw new IllegalStateException("Redis ping failed");
                    }
                }

                this.pool = newPool;
                log.info("Successfully connected to redis");

                if (connected.compareAndSet(false, true)) {
                    observers.forEach(RedisConnectionObserver::onConnected);
                }
                startHeartbeat();
                break;
            } catch (Exception e) {
                log.error("Failed to establish redis connection (attempt {} of {})", attempt + 1, MAX_RECONNECT_ATTEMPTS, e);
                if (attempt < MAX_RECONNECT_ATTEMPTS - 1) {
                    try {
                        Thread.sleep(RECONNECT_INTERVAL_MS); // ждем перед повторной попыткой
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("Reconnect interrupted", ie);
                    }
                } else {
                    throw new IllegalStateException("Failed to establish redis connection after " + MAX_RECONNECT_ATTEMPTS + " attempts", e);
                }
            }
        }
    }

    public Jedis withJedis() {
        val p = this.pool;

        if (p == null || p.isClosed()) {
            throw new RedisConnectionNotEstablishedException("Redis connection is not established");
        }

        long start = System.nanoTime();
        Jedis resource = p.getResource();

        ensureKeepAlive(resource);

        // Since we return the resource, we can only measure the time to GET the resource.
        // To measure the actual operations, we'd need a proxy or to wrap every call.
        // For now, let's track the time to get a connection from the pool as it can indicate pool exhaustion.
        RedisPerformanceMetrics.record(System.nanoTime() - start);
        return resource;
    }

    private void startHeartbeat() {
        if (heartbeatStarted.compareAndSet(false, true)) {
            heartbeatExecutor.scheduleAtFixedRate(this::checkConnection, 10, 10, TimeUnit.SECONDS);
        }
    }

    private void checkConnection() {
        try {
            val p = this.pool;
            if (p == null || p.isClosed()) {
                throw new Exception("Pool is null or closed");
            }

            try (val jedis = p.getResource()) {
                jedis.ping();
            }

            if (connected.compareAndSet(false, true)) {
                log.info("Redis connection restored");
                observers.forEach(RedisConnectionObserver::onConnected);
            }
        } catch (Exception e) {
            if (connected.compareAndSet(true, false)) {
                log.error("Redis connection lost: {}", e.getMessage());
                observers.forEach(RedisConnectionObserver::onDisconnected);
            }

            log.info("Heartbeat: Attempting to reconnect to Redis...");
            try {
                establish();
            } catch (Exception ex) {
                log.error("Heartbeat: Reconnect attempt failed: {}", ex.getMessage());
            }
        }
    }

    private void ensureKeepAlive(Jedis resource) {
        try {
            String pingResponse = resource.ping();
            if (!"PONG".equalsIgnoreCase(pingResponse)) {
                log.warn("Received unexpected ping response: {}", pingResponse);
            }
        } catch (Exception e) {
            log.warn("Failed to ping Redis for Keep-Alive", e);
        }
    }

    public boolean isClosed() {
        val p = this.pool;
        return p == null || p.isClosed();
    }

    public synchronized void shutdown() {
        heartbeatExecutor.shutdownNow();
        if (pool != null && !pool.isClosed()) {
            pool.close();
            log.info("Redis pool closed");
        }
        if (connected.compareAndSet(true, false)) {
            observers.forEach(RedisConnectionObserver::onDisconnected);
        }
    }
}
