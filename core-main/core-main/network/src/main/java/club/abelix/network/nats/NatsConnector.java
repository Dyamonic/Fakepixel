package club.abelix.network.nats;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;

@Slf4j
public abstract class NatsConnector {

    private Connection nc;
    private final String url;

    protected NatsConnector(String url) {
        this.url = url;
    }

    public void connect() {
        try {
            Options options = new Options.Builder()
                    .server(url)
                    .reconnectWait(Duration.ofSeconds(2))
                    .maxReconnects(-1)
                    .build();

            nc = Nats.connect(options);
            log.info("[{}] Connected to NATS: {}", getClass().getSimpleName(), url);
        } catch (IOException | InterruptedException e) {
            log.warn("[{}] Failed to connect to NATS: {}", getClass().getSimpleName(), url, e);
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
        }
    }

    protected void publish(String subject, byte[] data) {
        if (nc == null || nc.getStatus() != Connection.Status.CONNECTED) return;
        try {
            nc.publish(subject, data);
        } catch (Exception e) {
            log.warn("[{}] Failed to publish to NATS subject '{}': {}", getClass().getSimpleName(), subject, e.getMessage());
        }
    }

    public boolean isConnected() {
        return nc != null && nc.getStatus() == Connection.Status.CONNECTED;
    }

    public void close() {
        if (nc != null) {
            try {
                nc.close();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
