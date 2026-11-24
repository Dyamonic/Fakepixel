package club.abelix.core.app.connection.ping.task;

import club.abelix.core.app.connection.ping.ServerPingService;
import club.abelix.core.app.connection.server.AbstractConnectable;
import club.abelix.core.app.connection.server.MasterServer;
import club.abelix.core.app.connection.server.ServerManager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public final class ServersHealthCheckTask implements Runnable {

    private static final int PING_TIMEOUT_MS = 1000;

    ServerManager serverManager;
    ServerPingService pingService;

    @Override
    public void run() {
        val successServer = new AtomicInteger(0);
        val failedServer = new AtomicInteger(0);

        val totalSize = serverManager.getProxies().size() + serverManager.getServers().size();
        val servers = new HashSet<AbstractConnectable>(totalSize);

        servers.addAll(serverManager.getServers().stream().filter(server -> server instanceof MasterServer).toList());
        servers.addAll(serverManager.getProxies());

        val futures = new HashMap<AbstractConnectable, PingRequestData>(totalSize);
        val random = ThreadLocalRandom.current();

        servers.forEach(server -> {
            val randomInt = random.nextInt();
            val future = pingService.requestPing(server, randomInt).thenApply(res -> res == randomInt);
            val timestamp = System.currentTimeMillis();

            futures.put(server, new PingRequestData(
                    server,
                    randomInt,
                    timestamp,
                    future
            ));
        });

        futures.forEach((server, data) -> {
            data.future.whenComplete((success, e) -> {
                if (e != null) {
                    if (e instanceof TimeoutException) {
                        log.error("Server ping request timeout! {} will be removed from core!", server.getName());
                    }

                    server.recordPingFailure();
                    failedServer.incrementAndGet();

                    if (server.isUnhealthy()) {
                        log.error("Failed to ping server {}! Server will be removed from core!", server.getName(), e);
                        awaitDisconnect(server);
                    }

                    return;
                }

                if (!success) {
                    server.recordPingFailure();
                    failedServer.incrementAndGet();

                    if (server.isUnhealthy()) {
                        log.error("Server {} did not respond with the correct ping response! Server will be removed from core!", server.getName());
                        awaitDisconnect(server);
                    }

                    return;
                }

                val timeTaken = System.currentTimeMillis() - data.timestamp;

                if (timeTaken > PING_TIMEOUT_MS) {
                    log.error("Server {} responded to ping request but took too long!", server.getName());

                    server.recordPingFailure();
                    failedServer.incrementAndGet();

                    if (server.isUnhealthy()) {
                        log.error("Server {} did not respond to ping request in time! Server will be removed from core!", server.getName());
                        awaitDisconnect(server);
                    }

                    return;
                }

                server.getFailedPingAttempts().set(0);
                successServer.incrementAndGet();
            });
        });

        log.info("Sent ping requests to {} servers. Awaiting responses...", totalSize);

        CompletableFuture.allOf(futures.values().stream().map(PingRequestData::future).toArray(CompletableFuture[]::new))
                .whenComplete((v, e) -> log.info("Completed health check for servers! Success: {}, Failed: {}", successServer.get(), failedServer.get()));
    }

    private void awaitDisconnect(AbstractConnectable server) {
        serverManager.awaitDisconnect(server);
        log.warn("Server {} failed to respond to ping request and will await disconnect", server.getName());
    }

    private record PingRequestData(
            AbstractConnectable server,
            int randomInt,
            long timestamp,
            CompletableFuture<Boolean> future
    ) {

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;

            PingRequestData other = (PingRequestData) obj;

            return server.getName().equalsIgnoreCase(other.server.getName());
        }

        @Override
        public int hashCode() {
            return server.getName().toLowerCase().hashCode();
        }
    }
}