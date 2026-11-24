package club.abelix.core.app.data.stream.platform;

import club.abelix.commons.util.json.JsonBodyHandler;
import club.abelix.core.app.data.stream.detail.AbstractStreamDetails;
import club.abelix.core.app.data.stream.exception.StreamEndedException;
import club.abelix.core.app.data.stream.exception.StreamException;
import club.abelix.core.app.data.stream.exception.StreamNotFoundException;
import club.abelix.core.app.data.stream.responses.YouTubeVideosResponse;
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
public class YouTubeStreamPlatform implements StreamPlatform<YouTubeStreamPlatform.YouTubeStreamDetails> {

    private static final String YOUTUBE_STREAM_URL = "https://youtu.be/";
    private static final String YOUTUBE_API_URL = "https://www.googleapis.com/youtube/v3/videos";


    private static final Pattern YOUTUBE_VIDEO_PATTERN = Pattern.compile(
            "(https://(www\\.|)|www.)(youtube.com/watch\\?v=|youtu.be/)(.+)", Pattern.CASE_INSENSITIVE);

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    /**
     * API ключ для YouTube
     */
    private final String apiKey;

    @Override
    public YouTubeStreamDetails parseStreamUrl(@NonNull final String streamUrl) {
        Matcher matcher = YOUTUBE_VIDEO_PATTERN.matcher(streamUrl);

        if (!matcher.matches()) {
            return null;
        }

        return new YouTubeStreamDetails(matcher.group(4));
    }

    @Override
    public String makeBeautifulUrl(@NonNull final AbstractStreamDetails streamDetails) {
        return YOUTUBE_STREAM_URL + streamDetails.getIdentity();
    }

    @Override
    @SneakyThrows
    public Object makeRequest(@NonNull final String streamId) {
        String url = YOUTUBE_API_URL +
                "?key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8) +
                "&part=" + URLEncoder.encode("snippet,liveStreamingDetails", StandardCharsets.UTF_8) +
                "&id=" + URLEncoder.encode(streamId, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<YouTubeVideosResponse> response = HTTP_CLIENT.send(
                request,
                JsonBodyHandler.create(YouTubeVideosResponse.class)
        );

        if (response.statusCode() != 200) {
            log.error("Failed to request YouTube streams: status={}, body={}",
                    response.statusCode(),
                    response.body());
            return null;
        }

        YouTubeVideosResponse body = response.body();
        if (body == null) {
            return null;
        }

        return body;
    }

    @Override
    public void updateStreamDetails(
            final @NonNull AbstractStreamDetails streamDetails,
            final @NonNull Object response
    ) throws StreamException {
        YouTubeVideosResponse body = (YouTubeVideosResponse) response;

        if (!body.hasItems()) {
            throw new StreamNotFoundException();
        }

        YouTubeVideosResponse.Item videoDetails = body.items().get(0);

        val snippet = videoDetails.snippet();
        val live = videoDetails.liveStreamingDetails();

        if (live == null) {
            throw new StreamException("youtube_api_error");
        }

        if (live.actualEndTime() != null) {
            throw new StreamEndedException();
        }

        streamDetails.setTitle(snippet.title());

        val viewers = live.concurrentViewersAsInt();
        streamDetails.setViewers(viewers != null ? viewers : 0);

        String startedAt = live.actualStartTime();
        if (startedAt != null) {
            streamDetails.setStartedAtServiceTime(Instant.parse(startedAt).toEpochMilli());
        }
    }

    @Override
    public String getDisplayName() {
        return "§cYou§fTube";
    }

    @Getter
    class YouTubeStreamDetails extends AbstractStreamDetails {

        private final String identity;

        YouTubeStreamDetails(@NonNull final String identity) {
            super(YouTubeStreamPlatform.this);
            this.identity = identity;
        }
    }
}
