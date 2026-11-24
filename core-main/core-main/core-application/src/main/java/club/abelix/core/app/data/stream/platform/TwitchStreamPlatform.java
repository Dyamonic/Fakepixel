package club.abelix.core.app.data.stream.platform;

import club.abelix.commons.util.json.JsonBodyHandler;
import club.abelix.core.app.data.stream.detail.AbstractStreamDetails;
import club.abelix.core.app.data.stream.exception.StreamException;
import club.abelix.core.app.data.stream.exception.StreamNotFoundException;
import club.abelix.core.app.data.stream.responses.TwitchStreamsResponse;
import club.abelix.core.app.data.stream.service.TwitchAuthService;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
public class TwitchStreamPlatform implements StreamPlatform<TwitchStreamPlatform.TwitchStreamDetails> {

    private static final String TWITCH_STREAM_URL = "https://twitch.tv/";
    private static final String TWITCH_API_URL = "https://api.twitch.tv/helix/streams";

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    /**
     * Поддерживает:
     *  - twitch.tv/nick
     *  - www.twitch.tv/nick
     *  - https://twitch.tv/nick
     *  - https://www.twitch.tv/nick
     *  и т.п.
     */
    private static final Pattern TWITCH_STREAM_PATTERN = Pattern.compile(
            "^(?:https?://)?(?:www\\.)?twitch\\.tv/([^/]+)", Pattern.CASE_INSENSITIVE);

    private final String clientId;

    @Override
    public TwitchStreamDetails parseStreamUrl(@NonNull String streamUrl) {
        Matcher matcher = TWITCH_STREAM_PATTERN.matcher(streamUrl);

        if (!matcher.find()) {
            return null;
        }

        String identity = matcher.group(1);
        return new TwitchStreamDetails(identity);
    }

    @Override
    public String makeBeautifulUrl(@NonNull AbstractStreamDetails streamDetails) {
        return TWITCH_STREAM_URL + streamDetails.getIdentity();
    }

    @Override
    @SneakyThrows
    public Object makeRequest(@NonNull String streamId) {
        val token = TwitchAuthService.getInstance().getAccessToken();

        if (token == null || token.isEmpty()) {
            return null;
        }

        String url = TWITCH_API_URL + "?user_login=" +
                URLEncoder.encode(streamId, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Client-Id", clientId)
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<TwitchStreamsResponse> response = HTTP_CLIENT.send(
                request,
                JsonBodyHandler.create(TwitchStreamsResponse.class)
        );

        if (response.statusCode() != 200) {
            log.error("Failed to request Twitch streams: status={}, body={}",
                    response.statusCode(),
                    response.body());
            return null;
        }

        TwitchStreamsResponse body = response.body();
        if (body == null) {
            return null;
        }

        if (body.error() != null || (body.status() != null && body.status() != 200)) {
            log.error("Twitch API returned error: status={}, error={}, message={}",
                    body.status(),
                    body.error(),
                    body.message());
            return null;
        }

        return body;
    }

    @Override
    public void updateStreamDetails(
            final @NonNull AbstractStreamDetails details,
            final @NonNull Object response
    ) throws StreamException {
        TwitchStreamsResponse body = (TwitchStreamsResponse) response;

        if (body == null) {
            throw new StreamException("twitch_api_error");
        }

        var data = body.data();
        if (data == null) {
            throw new StreamException("twitch_api_error");
        }

        if (data.isEmpty()) {
            throw new StreamNotFoundException();
        }

        TwitchStreamsResponse.TwitchStreamData stream = data.get(0);

        details.setTitle(stream.title());
        details.setViewers(stream.viewerCount() != null ? stream.viewerCount() : 0);

        String startedAt = stream.startedAt();
        if (startedAt != null) {
            details.setStartedAtServiceTime(Instant.parse(startedAt).toEpochMilli());
        }
    }

    @Override
    public String getDisplayName() {
        return "§3Twitch";
    }

    @Getter
    class TwitchStreamDetails extends AbstractStreamDetails {

        private final String identity;

        TwitchStreamDetails(@NonNull String identity) {
            super(TwitchStreamPlatform.this);
            this.identity = identity;
        }
    }
}
