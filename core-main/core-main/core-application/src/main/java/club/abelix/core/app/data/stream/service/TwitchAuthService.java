package club.abelix.core.app.data.stream.service;

import club.abelix.core.app.data.stream.platform.settings.StreamPlatformSettings;
import club.abelix.core.app.data.stream.responses.TwitchTokenResponse;
import club.abelix.commons.util.json.JsonBodyHandler;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.ReentrantLock;

@RequiredArgsConstructor
@Slf4j
public class TwitchAuthService {

    private static final String TWITCH_TOKEN_URL = "https://id.twitch.tv/oauth2/token";
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    @NonNull
    private final String clientId;
    @NonNull
    private final String clientSecret;

    private volatile String accessToken;
    private volatile long expiresAt;

    private final ReentrantLock lock = new ReentrantLock();

    private static final TwitchAuthService INSTANCE = new TwitchAuthService(
            StreamPlatformSettings.TWITCH_CLIENT_ID,
            StreamPlatformSettings.TWITCH_CLIENT_SECRET
    );

    public static TwitchAuthService getInstance() {
        return INSTANCE;
    }

    public String getAccessToken() {
        val now = System.currentTimeMillis();

        if (accessToken != null && now < expiresAt) {
            return accessToken;
        }

        lock.lock();
        try {
            val nowInside = System.currentTimeMillis();

            if (accessToken != null && nowInside < expiresAt) {
                return accessToken;
            }

            refreshTokenInternal();
            return accessToken;
        } finally {
            lock.unlock();
        }
    }

    @SneakyThrows
    private void refreshTokenInternal() {
        val form = "client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8) +
                "&grant_type=" + URLEncoder.encode("client_credentials", StandardCharsets.UTF_8);

        val request = HttpRequest.newBuilder()
                .uri(URI.create(TWITCH_TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(BodyPublishers.ofString(form))
                .build();

        val handler = JsonBodyHandler.create(TwitchTokenResponse.class);
        val httpResponse = HTTP_CLIENT.send(request, handler);
        val body = httpResponse.body();

        if (body == null) {
            log.error("Failed to get Twitch app access token: body is null");
            return;
        }

        if (httpResponse.statusCode() != 200 || (body.status() != null && body.status() != 200)) {
            log.error("Failed to get Twitch app access token: httpStatus={}, status={}, message={}", httpResponse.statusCode(), body.status(), body.message());
            return;
        }

        val token = body.accessToken();
        val expiresIn = body.expiresIn();

        if (token == null || token.isEmpty()) {
            log.error("Failed to get Twitch app access token: 'access_token' is null or empty, body={}", body);
            return;
        }

        if (expiresIn == null) {
            log.error("Failed to get Twitch app access token: 'expires_in' is null, body={}", body);
            return;
        }

        val now = System.currentTimeMillis();
        val safetyDelta = 60_000L;
        var newExpiresAt = now + expiresIn * 1000L - safetyDelta;

        if (newExpiresAt <= now) {
            newExpiresAt = now + 5 * 60_000L;
        }

        this.accessToken = token;
        this.expiresAt = newExpiresAt;

        log.info("Successfully refreshed Twitch app access token, expiresAt={}", newExpiresAt);
    }
}
