package club.abelix.core.votifer.nats;

import club.abelix.commons.data.vote.VotePlatform;
import club.abelix.commons.data.vote.VoteSqlHandler;
import club.abelix.commons.util.json.JsonUtility;
import club.abelix.core.votifer.nats.model.Vote;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import io.nats.client.Options;
import lombok.RequiredArgsConstructor;
import club.abelix.core.votifer.VotifierCorePlugin;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Slf4j
@RequiredArgsConstructor
public class VotifierNats {

    private static final String DEFAULT_NATS_URL = "nats://127.0.0.1:4222";
    private static final String CONNECTION_NAME = "core-votifier";
    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration DRAIN_TIMEOUT = Duration.ofSeconds(2);

    private final VotifierCorePlugin plugin;
    private final String subject;
    private Connection connection;
    private Dispatcher dispatcher;

    public void start() {
        String natsUrl = getNatsUrl();

        try {
            connection = createConnection(natsUrl);
            dispatcher = createDispatcher(connection);
            dispatcher.subscribe(subject);

            log.info("Votifier NATS listener subscribed to '{}' using {}", subject, natsUrl);
        } catch (InterruptedException | IOException exception) {
            log.error("Failed to start Votifier NATS listener for subject '{}'", subject, exception);
        }
    }

    public void stop() {
        try {
            unsubscribeDispatcher();
            closeConnection();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (TimeoutException exception) {
            log.warn("Timed out while draining Votifier NATS connection for subject '{}'", subject, exception);
        }
    }

    private String getNatsUrl() {
        return System.getenv().getOrDefault("NATS_URL", DEFAULT_NATS_URL);
    }

    private Connection createConnection(String natsUrl) throws InterruptedException, IOException {
        Options options = new Options.Builder()
                .server(natsUrl)
                .connectionName(CONNECTION_NAME)
                .maxReconnects(-1)
                .connectionTimeout(CONNECTION_TIMEOUT)
                .build();

        return Nats.connect(options);
    }

    private Dispatcher createDispatcher(Connection connection) {
        return connection.createDispatcher(message -> processMessage(message.getData()));
    }

    private void processMessage(byte[] payload) {
        try {
            Vote vote = JsonUtility.MAPPER.readValue(payload, Vote.class);
            handleVote(vote);
        } catch (Exception exception) {
            log.error("Failed to process vote from NATS payload", exception);
        }
    }

    private void unsubscribeDispatcher() {
        if (dispatcher == null) {
            return;
        }

        dispatcher.unsubscribe(subject);
        dispatcher = null;
    }

    private void closeConnection() throws InterruptedException, TimeoutException {
        if (connection == null) {
            return;
        }

        if (connection.getStatus() == Connection.Status.CONNECTED) {
            connection.drain(DRAIN_TIMEOUT);
        }

        connection.close();
        connection = null;
    }

    private void handleVote(Vote vote) {
        if (vote == null || vote.getUsername() == null || vote.getServiceName() == null) {
            return;
        }

        VotePlatform platform = resolvePlatform(vote.getServiceName());

        if (platform == null) {
            log.warn("Unknown vote platform '{}', username={}", vote.getServiceName(), vote.getUsername());
            return;
        }

        plugin.getVoteManager().processVote(platform, vote.getUsername());
    }

    private VotePlatform resolvePlatform(String rawServiceName) {
        String serviceName = rawServiceName.trim();

        if (serviceName.isEmpty()) {
            return null;
        }

        return VoteSqlHandler.INSTANCE.getVotePlatform(serviceName);
    }

}
