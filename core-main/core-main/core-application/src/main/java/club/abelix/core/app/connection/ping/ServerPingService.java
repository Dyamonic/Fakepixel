package club.abelix.core.app.connection.ping;

import club.abelix.core.app.connection.server.AbstractConnectable;
import club.abelix.network.packet.play.client.CServerPingResponse;
import club.abelix.network.packet.play.server.SServerPingRequest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class ServerPingService {

    public CompletableFuture<Integer> requestPing(
            AbstractConnectable server,
            int randomId
    ) {
        return server.getConnection()
                .awaitPacket(new SServerPingRequest(randomId))
                .thenApply(CServerPingResponse::getRandom)
                .orTimeout(10, TimeUnit.SECONDS);
    }

}
