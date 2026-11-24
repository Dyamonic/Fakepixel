package club.abelix.core.velocity.chat;

import club.abelix.network.nats.NatsConnector;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import w.util.Env;

import java.nio.charset.StandardCharsets;

@Slf4j
public class NatsChatPublisher extends NatsConnector {

    private static final String SUBJECT_PREFIX = "chat.messages.";
    private static final Gson GSON = new Gson();

    public NatsChatPublisher() {
        super(Env.getString("NATS_URL", "nats://localhost:4222"));
    }

    public void publish(String type, String playerName, String server, String message) {
        log.info("[NatsChatPublisher] publish: player={} type={} server={} connected={}",
                playerName, type, server, isConnected());
        var payload = new ChatMessagePayload(playerName, message, server, System.currentTimeMillis(), type);
        publish(SUBJECT_PREFIX + type, GSON.toJson(payload).getBytes(StandardCharsets.UTF_8));
    }

    private record ChatMessagePayload(String playerName, String message, String server, long timestamp, String type) {}
}
