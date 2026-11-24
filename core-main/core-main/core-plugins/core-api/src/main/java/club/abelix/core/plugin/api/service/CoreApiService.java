package club.abelix.core.plugin.api.service;

import club.abelix.commons.data.stat.StatsManager;
import club.abelix.commons.data.stat.StatsSqlHandler;
import club.abelix.core.api.proto.v1.CoreStatusResponse;
import club.abelix.core.api.proto.v1.GenerateTopRequest;
import club.abelix.core.api.proto.v1.GenerateTopResponse;
import club.abelix.core.api.proto.v1.GetOnlineResponse;
import club.abelix.core.app.CoreApplication;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.gamer.Gamer;
import io.grpc.Status;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class CoreApiService {

    ServerManager serverManager;
    CoreApplication coreApplication;

    public GetOnlineResponse getGlobalOnline() {
        return GetOnlineResponse.newBuilder()
                .setOnline(serverManager.getTotalOnline())
                .build();
    }

    public CoreStatusResponse getCoreStatus() {
        long uptime = coreApplication.getStartTime() != null
                ? System.currentTimeMillis() - coreApplication.getStartTime()
                : 0L;
        return CoreStatusResponse.newBuilder()
                .setUptime(uptime)
                .build();
    }

    public CompletableFuture<GenerateTopResponse> generateTop(GenerateTopRequest request) {
        val statsMode = StatsManager.INSTANCE.tryGetMode(request.getMode());

        if (statsMode == null) throw Status.INVALID_ARGUMENT.withDescription("Mode not found").asRuntimeException();

        val field = statsMode.getField(request.getStatsField());

        if (field == null) throw Status.INVALID_ARGUMENT.withDescription("Field not found").asRuntimeException();

        return StatsSqlHandler.INSTANCE.getTopAllTime(field, 10, false).thenApply(top -> {
            val result = new LinkedHashMap<String, Integer>(10);

            top.forEach(topPair -> {
                val gamer = Gamer.getOfflineGamer(topPair.getFirstInt(), Gamer::load);

                if (gamer == null) {
                    return;
                }

                val value = topPair.getSecondInt();

                result.put(gamer.getDisplayName(), value);
            });

            return GenerateTopResponse.newBuilder()
                    .putAllTop(result)
                    .setMode(request.getMode())
                    .build();
        });

    }
}
