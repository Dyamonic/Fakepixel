package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.util.json.JsonBodyHandler;
import club.abelix.commons.util.json.JsonBodyPublisher;
import club.abelix.commons.util.json.JsonUtility;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.api.dto.KeyCreateRequest;
import club.abelix.core.app.data.api.dto.KeyCreationResponse;
import club.abelix.core.app.data.gamer.Gamer;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import w.util.Env;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@WithAlias("api")
@NamedCommand("apikey")
public class ApiKeyCommand {

    private static final String API_URL = Env.getString("PUBLIC_API_SERVICE");
    private static final String INTERNAL_TOKEN = Env.getString("INTERNAL_API_TOKEN");
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final long MIN_PLAY_TIME = TimeUnit.HOURS.toSeconds(10);

    @Executor
    public void execute(final @NotNull CoreExecutionSession session) {
        val gamer = session.getGamer();

        if (gamer.getGlobalTime() < MIN_PLAY_TIME) {
            session.sendLocalizedMessage("api_key_command_insufficient_play_time", "%minPlayTimeHours%", String.valueOf(MIN_PLAY_TIME / 3600));
            return;
        }

        session.sendMessage("Generating your API key, please wait...");

        val key = generateApiKey(gamer);

        session.sendLocalizedMessage("api_key_command_success", "%key%", key.key());
    }

    private KeyCreationResponse generateApiKey(final @NotNull Gamer gamer) {
        val requestDto = new KeyCreateRequest(gamer.getPlayerId());
        val request = HttpRequest.newBuilder(URI.create(API_URL + "/internal/api/v1/key/create"))
                .POST(JsonBodyPublisher.create(requestDto))
                .header("X-Internal-Token", INTERNAL_TOKEN)
                .header("Content-Type", "application/json")
                .build();

        try {
            val response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            val body = response.body();
            if (response.statusCode() == 201) {
                return JsonUtility.fromJson(body, KeyCreationResponse.class);
            } else {
                throw new RuntimeException("Failed to generate API key: " + body);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate API key", e);
        }
    }

}
