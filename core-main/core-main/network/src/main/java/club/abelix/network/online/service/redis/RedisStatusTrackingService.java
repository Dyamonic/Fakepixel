package club.abelix.network.online.service.redis;

import club.abelix.network.online.data.Keys;
import club.abelix.network.online.field.ServerField;
import club.abelix.network.online.info.ServerInfo;
import club.abelix.network.online.info.TrackedServer;
import club.abelix.network.online.service.StatusTrackingService;
import club.abelix.network.online.task.StatusUpdaterTask;
import club.abelix.network.online.updater.ServerStatusUpdater;
import club.abelix.network.redis.JedisConnector;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Pipeline;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class RedisStatusTrackingService implements StatusTrackingService {

    private static final int REDIS_DB_INDEX = 1;
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    JedisConnector connector;

    Map<String, TrackedServer> tracked = new ConcurrentHashMap<>();
    Map<String, TaskPair> tasksById = new ConcurrentHashMap<>();

    @Override
    public void track(
            final @NotNull ServerInfo serverInfo,
            final @NotNull ServerStatusUpdater updater
    ) {
        if (tracked.containsKey(serverInfo.getId())) {
            log.warn("Status for {} already tracked!", serverInfo.getId());
            return;
        }

        pushStatus(serverInfo);

        val task = new StatusUpdaterTask(
                this,
                updater,
                serverInfo
        );

        val future = EXECUTOR.scheduleAtFixedRate(
                task,
                10,
                10,
                TimeUnit.SECONDS
        );

        tasksById.put(serverInfo.getId(), new TaskPair(task, future));
        tracked.put(serverInfo.getId(), new TrackedServer(serverInfo));
    }

    @Override
    public void restore(@NotNull ServerInfo serverInfo) {
        pushStatus(serverInfo);
        log.info("Restored tracking for server {}", serverInfo.getId());
    }

    @Override
    public void restoreAll() {
        tracked.forEach((serverName, trackedServer) -> {
            log.info("Restoring tracking for server {}", serverName);
            pushStatus(trackedServer.getServerInfo().get());

        });
    }

    private void pushStatus(final @NotNull ServerInfo serverInfo) {
        try (val jedis = connector.withJedis()) {
            jedis.select(REDIS_DB_INDEX);

            val id = serverInfo.getId();
            val key = Keys.server(id);
            val allFields = new HashMap<String, String>();

            serverInfo.getStaticData().forEach((field, value) -> {
                if (value != null) {
                    allFields.put(field.getFieldName(), value.toLowerCase(Locale.ROOT));
                }
            });

            serverInfo.getDynamicData().forEach((field, value) -> {
                if (value != null) {
                    allFields.put(field.getFieldName(), value.toLowerCase(Locale.ROOT));
                }
            });

            val pipe = jedis.pipelined();

            updateIndexes(serverInfo.getStaticData(), id, pipe);

            pipe.hset(key, allFields);
            pipe.expire(key, 30);

            pipe.sadd(Keys.ALL_SERVERS_INDEX, id);

            pipe.sync();

            log.info("Initialized tracking for server {}", id);

            update(serverInfo);
        }
    }

    @Override
    public void update(
            final @NotNull ServerInfo serverInfo
    ) {
        try (val jedis = connector.withJedis()) {
            jedis.select(REDIS_DB_INDEX);

            val pipeline = jedis.pipelined();

            updateIndexes(serverInfo.getDynamicData(), serverInfo.getId(), pipeline);

            val dynamicFields = new HashMap<String, String>(serverInfo.getDynamicData().size());

            serverInfo.getDynamicData().forEach((field, value) -> {
                if (value != null) {
                    dynamicFields.put(field.getFieldName(), value.toLowerCase(Locale.ROOT));
                }
            });

            pipeline.hset(Keys.server(serverInfo.getId()), dynamicFields);

            pipeline.expire(Keys.server(serverInfo.getId()), 30);

            pipeline.sync();
        }
    }

    @Override
    public void stop(
            final @NotNull String id
    ) {
        val tasks = tasksById.get(id);
        val info = tracked.get(id);

        if (tasks == null) {
            log.error("No tracking task found for server {}", info.getId());
            return;
        }

        tasks.future().cancel(false);

        tracked.remove(id);
        tasksById.remove(id);

        val fields = new HashMap<ServerField, String>();
        val serverInfo = info.getServerInfo()
                .get();

        serverInfo.getStaticData()
                .forEach((field, value) -> {
                    if (value != null && field.isIndexed()) {
                        fields.put(field, value);
                    }
                });

        serverInfo.getDynamicData()
                .forEach((field, value) -> {
                    if (value != null && field.isIndexed()) {
                        fields.put(field, value);
                    }
                });

        try (val jedis = connector.withJedis()) {
            jedis.select(REDIS_DB_INDEX);

            val key = Keys.server(id);
            val pipe = jedis.pipelined();

            pipe.del(key);
            pipe.srem(Keys.ALL_SERVERS_INDEX, id.toLowerCase(Locale.ROOT));

            log.debug("Deleted server key and removed from all servers index for server {}", id);

            for (val entry : fields.entrySet()) {
                val field = entry.getKey();
                val value = entry.getValue();

                if (field.isMultiValued()) {
                    if (value != null) {
                        for (val part : value.split(",")) {
                            val trimmed = part.trim();

                            if (!trimmed.isEmpty()) {
                                pipe.srem(Keys.index(field, trimmed), id.toLowerCase(Locale.ROOT));
                            }
                        }
                    }
                } else {
                    if (value != null) {
                        pipe.srem(Keys.index(field, value), id.toLowerCase(Locale.ROOT));
                    }
                }

                log.debug("Removed index for server {}: {} -> {}", id, field.getFieldName(), value);
            }

            pipe.sync();
        }

        log.info("Stopped tracking for server {}", info.getId());
    }

    @Override
    public void shutdown() {
        tracked.forEach((k, __) -> stop(k));
        EXECUTOR.shutdown();

        log.info("RedisStatusTrackingService shutdown complete.");
    }

    private void updateIndexes(
            final @NotNull Map<ServerField, String> data,
            final @NotNull String id,
            final @NotNull Pipeline pipeline
    ) {
        data.forEach((field, value) -> {
            if (value == null || !field.isIndexed()) return;

            if (field.isMultiValued()) {
                for (val part : value.split(",")) {
                    val trimmed = part.trim();

                    if (!trimmed.isEmpty()) {
                        pipeline.sadd(Keys.index(field, trimmed), id.toLowerCase(Locale.ROOT));
                    }
                }
            } else {
                pipeline.sadd(Keys.index(field, value), id.toLowerCase(Locale.ROOT));
            }

            log.debug("Updated index for server {}: {} -> {}", id, field.getFieldName(), value);
        });
    }

    private record TaskPair(
            StatusUpdaterTask task,
            ScheduledFuture<?> future
    ) {
    }
}
