package club.abelix.core.plugin.keda;

import club.abelix.commons.util.EnumUtils;
import club.abelix.commons.util.json.JsonUtility;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.plugin.keda.dto.KedaFillrateResponse;
import club.abelix.network.common.enums.GameState;
import club.abelix.network.online.data.FieldMatchMode;
import club.abelix.network.online.data.ServerKind;
import club.abelix.network.online.field.StandardServerFields;
import club.abelix.network.online.query.StatusQueries;
import club.abelix.network.online.query.StatusQueryResult;
import club.abelix.network.online.query.execution.StatusQueryExecutionService;
import club.abelix.network.type.mode.Mini;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class KedaWebServerHandler implements HttpHandler {

    StatusQueryExecutionService statusQueryExecutionService;

    @Override
    public void handle(HttpExchange t) {
        val serverManager = ServerManager.IMP;
        val values = new HashMap<String, Map<String, Integer>>();

        val query = StatusQueries.parametrizedQuery()
                .kind(ServerKind.ARENA)
                .tag("MINI")
                .matchMode(StandardServerFields.TAG, FieldMatchMode.ALL)
                .withDetails()
                .build();

        try {
            val res = statusQueryExecutionService.executeSync(query, true);
            val minisByMini = new HashMap<Mini, List<StatusQueryResult.ArenaPerServer>>();

            for (val ser : res.getPerServer().values()) {
                if (!(ser instanceof StatusQueryResult.ArenaPerServer arenaPerServer)) {
                    continue;
                }

                val mode = ser.getMode();
                if (mode == null) {
                    continue;
                }

                val upper = mode.toUpperCase();

                if (!EnumUtils.isInEnum(Mini.class, upper)) {
                    continue;
                }

                val mini = Mini.valueOf(upper);
                minisByMini
                        .computeIfAbsent(mini, k -> new ArrayList<>())
                        .add(arenaPerServer);
            }

            for (val entry : minisByMini.entrySet()) {
                val mini = entry.getKey();
                val miniServers = entry.getValue();

                try {
                    if (miniServers.isEmpty()) {
                        continue;
                    }

                    val allServerCount = miniServers.size();

                    val notFreeServerCount = miniServers.stream().filter(server -> {
                        val coreServer = serverManager.getServer(server.getName());

                        if (coreServer == null) {
                            return true;
                        }

                        if (!coreServer.canJoin()) {
                            return true;
                        }

                        try {
                            return GameState.valueOf(server.getArenaState().toUpperCase()) != GameState.STAND_BY;
                        } catch (IllegalArgumentException e) {
                            log.warn("Unknown arena state '{}' for server {}", server.getArenaState(), server.getId());
                            return true;
                        }
                    }).count();

                    val fill = 100.0 * notFreeServerCount / (double) allServerCount;
                    val fillRate = (int) Math.round(fill);

                    values
                            .computeIfAbsent(mini.getMaster().getName(), k -> new HashMap<>())
                            .put(mini.getName(), fillRate);
                } catch (Exception e) {
                    log.error("Failed calculate fill rate for mini {}", mini.getMaster().getName(), e);
                }
            }

            log.debug("Keda fillrate values: {}", values);
            log.debug("Keda fillrate response: {}", JsonUtility.toJson(new KedaFillrateResponse(values)));

            sendJson(t, 200, new KedaFillrateResponse(values));
        } catch (Exception e) {
            log.error("Failed to handle Keda fillrate request", e);
            sendJson(t, 500, Map.of("error", "internal_error"));
        }
    }


    private void sendJson(
            final @NotNull HttpExchange t,
            final int status,
            final @NotNull Object body
    ) {
        try {
            val response = JsonUtility.toJson(body);
            val bytes = response.getBytes(StandardCharsets.UTF_8);

            t.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            t.sendResponseHeaders(status, bytes.length);

            try (OutputStream os = t.getResponseBody()) {
                os.write(bytes);
            }
        } catch (Exception e) {
            log.error("Failed to send HTTP response", e);
            try {
                t.sendResponseHeaders(500, -1);
            } catch (IOException ignored) {
            } finally {
                try {
                    t.getResponseBody().close();
                } catch (IOException ignored) {
                }
            }
        }
    }

}
