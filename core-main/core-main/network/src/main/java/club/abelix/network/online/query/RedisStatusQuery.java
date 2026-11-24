package club.abelix.network.online.query;

import club.abelix.network.online.data.FieldMatchMode;
import club.abelix.network.online.data.Keys;
import club.abelix.network.online.field.ServerField;
import club.abelix.network.online.field.StandardServerFields;
import club.abelix.network.redis.JedisConnector;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class RedisStatusQuery implements StatusQuery<JedisConnector> {

    static final int REDIS_DB_INDEX = 1;

    StatusQueryParams params;

    @Override
    public @NotNull StatusQueryResult execute(final @NotNull JedisConnector connector) {
        try (val jedis = connector.withJedis()) {
            jedis.select(REDIS_DB_INDEX);

            val candidateIds = collectCandidateIds(jedis);

            if (candidateIds.isEmpty()) {
                log.debug("No servers matched filters: {}", params);
                return StatusQueryResult.EMPTY;
            }

            if (params.isTotalOnly()) {
                val total = fetchTotalOnlineOnly(jedis, candidateIds);

                return new StatusQueryResult(total, Set.of("global"), Map.of(), false);
            } else {
                return fetchDetailed(jedis, candidateIds);
            }
        } catch (Exception e) {
            log.error("Error executing status query with params {}", params, e);
            throw new RuntimeException("Failed to execute status query", e);
        }
    }

    @Override
    public @NotNull StatusQueryParams params() {
        return params;
    }

    private Set<String> collectCandidateIds(final @NotNull Jedis jedis) {
        val candidateIds = new HashSet<>(params.getServerIds());

        val indexedFilters = params.getEqualsFilters().entrySet().stream()
                .filter(e -> e.getKey().isIndexed())
                .toList();

        if (!indexedFilters.isEmpty()) {
            val pipe = jedis.pipelined();
            val responses = new HashMap<ServerField, List<Response<Set<String>>>>();

            for (val entry : indexedFilters) {
                val field = entry.getKey();
                val values = entry.getValue();

                val list = new ArrayList<Response<Set<String>>>();
                responses.put(field, list);

                for (val value : values) {
                    val indexKey = Keys.index(field, value);

                    list.add(pipe.smembers(indexKey));
                }
            }

            pipe.sync();

            Set<String> filtered = null;

            for (val entry : responses.entrySet()) {
                val field = entry.getKey();
                val respList = entry.getValue();
                val mode = params.matchMode(field);

                Set<String> perField = null;

                for (val resp : respList) {
                    val ids = resp.get();

                    if (ids == null || ids.isEmpty()) {
                        continue;
                    }

                    if (perField == null) {
                        perField = new HashSet<>(ids);
                    } else if (mode == FieldMatchMode.ANY) {
                        perField.addAll(ids);
                    } else {
                        perField.retainAll(ids);
                    }
                }

                if (perField == null) {
                    return new HashSet<>();
                }

                if (filtered == null) {
                    filtered = perField;
                } else {
                    filtered.retainAll(perField);
                }
            }

            if (filtered != null) {
                if (candidateIds.isEmpty()) {
                    candidateIds.addAll(filtered);
                } else {
                    candidateIds.retainAll(filtered);
                }

                if (candidateIds.isEmpty()) {
                    return candidateIds;
                }
            }
        }

        if (candidateIds.isEmpty() && !params.hasFilters()) {
            candidateIds.addAll(jedis.smembers(Keys.ALL_SERVERS_INDEX));
        }

        return candidateIds;
    }

    private int fetchTotalOnlineOnly(
            final @NotNull Jedis jedis,
            final @NotNull Set<String> ids
    ) {
        val pipe = jedis.pipelined();
        val responses = new HashMap<String, Response<String>>();

        for (val id : ids) {
            val key = Keys.server(id);
            val resp = pipe.hget(key, StandardServerFields.ONLINE.getFieldName());

            responses.put(id, resp);
        }

        pipe.sync();

        var total = 0;

        for (val entry : responses.entrySet()) {
            val raw = entry.getValue().get();

            total += parseIntSafe(raw);
        }

        return total;
    }

    private StatusQueryResult fetchDetailed(
            final @NotNull Jedis jedis,
            final @NotNull Set<String> ids
    ) {
        val pipe = jedis.pipelined();
        val responses = new LinkedHashMap<String, Response<List<String>>>();

        val fieldsToLoad = List.of(
                StandardServerFields.NAME,
                StandardServerFields.ONLINE,
                StandardServerFields.VANISHED_ONLINE,
                StandardServerFields.SLOTS,
                StandardServerFields.MODE,
                StandardServerFields.ARENA_STATE,
                StandardServerFields.MAP_NAME
        );

        val fieldNames = fieldsToLoad.stream()
                .map(ServerField::getFieldName)
                .toArray(String[]::new);

        for (val id : ids) {
            val key = Keys.server(id);
            val resp = pipe.hmget(key, fieldNames);
            responses.put(id, resp);
        }

        pipe.sync();

        var totalOnline = 0;
        val perServer = new LinkedHashMap<String, StatusQueryResult.PerServer>();

        for (val entry : responses.entrySet()) {
            val id = entry.getKey();
            val values = entry.getValue().get();

            if (values == null || values.isEmpty()) {
                continue;
            }

            val fieldMap = new HashMap<ServerField, String>();
            for (int i = 0; i < fieldsToLoad.size(); i++) {
                fieldMap.put(fieldsToLoad.get(i), values.get(i));
            }

            val name = fieldMap.get(StandardServerFields.NAME);
            val online = parseIntSafe(fieldMap.get(StandardServerFields.ONLINE));
            val vanished = parseIntSafe(fieldMap.get(StandardServerFields.VANISHED_ONLINE));
            val slots = parseIntSafe(fieldMap.get(StandardServerFields.SLOTS));
            val mode = fieldMap.get(StandardServerFields.MODE);

            val arenaState = fieldMap.get(StandardServerFields.ARENA_STATE);
            val mapName = fieldMap.get(StandardServerFields.MAP_NAME);

            val isArena = arenaState != null && mapName != null;

            totalOnline += online;

            val parentServer = new StatusQueryResult.PerServer(
                    id,
                    name != null ? name : id,
                    mode,
                    online,
                    vanished,
                    slots
            );

            StatusQueryResult.PerServer server;
            if (isArena) {
                server = StatusQueryResult.ArenaPerServer.ofParent(
                        parentServer,
                        mapName,
                        arenaState
                );
            } else {
                server = parentServer;
            }

            perServer.put(id, server);
        }

        return new StatusQueryResult(totalOnline, perServer.keySet(), perServer, false);
    }

    private static int parseIntSafe(
            final String value
    ) {
        if (value == null) {
            return 0;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignore) {
            return 0;
        }
    }
}
