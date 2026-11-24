package club.abelix.core.plugin.forum.util;

import club.abelix.commons.data.rank.UserRanks;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.plugin.forum.config.RankSectionConfig;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class XenApi {

    public static int MINIMUM_PLAY_TIME = 1200;
    private static final Gson GSON = new Gson();

    String forumApi, forumKey;

    Map<Integer, Integer> forumRankMap = new HashMap<>();

    HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();


    private String form(Map<String, ?> params) {
        return params.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" +
                        URLEncoder.encode(String.valueOf(e.getValue()), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    public CompletableFuture<XfResult> ping() {
        return get("/index");
    }

    public void loadRanks(List<RankSectionConfig> rankSectionConfigs) {
        for (val section : rankSectionConfigs) {
            forumRankMap.put(section.getId(), section.getForumId());
        }
    }

    public int getForumGroup(UserRanks ranks) {
        val localRankId = ranks.findPriorityRank().rank().getId();

        if (!forumRankMap.containsKey(localRankId)) {
            throw new RuntimeException("Rank with id " + localRankId + " not found");
        }

        return forumRankMap.get(ranks.findPriorityRank().rank().getId());
    }

    public int getForumGroup(Gamer gamer) {
        return getForumGroup(gamer.getRanks());
    }

    public CompletableFuture<XfResult> createAccount(
            @NonNull String username,
            @NonNull String email,
            @NonNull String password,
            @NonNull Gamer gamer
    ) {
        val xfGroup = getForumGroup(gamer);
        val body = form(Map.of(
                "username", username,
                "email", email,
                "password", password,
                "user_group_id", Integer.toString(xfGroup)
        ));

        return post("/users", body);
    }

    public CompletableFuture<XfResult> updateUsername(
            long userId,
            @NonNull String newUsername
    ) {
        val body = form(Map.of(
                "username", newUsername,
                "username_change_visible", "1"
        ));

        return post("/users/" + userId, body);
    }

    public void setGroup(int xfUserId, @NonNull Gamer gamer) {

        val body = form(Map.of(
                "user_group_id", Integer.toString(getForumGroup(gamer))
        ));

        post("/users/" + xfUserId, body);
    }

    public boolean canRegister(@NonNull Gamer gamer) {
        return gamer.getGlobalTime() >= MINIMUM_PLAY_TIME;
    }

    private CompletableFuture<XfResult> get(final @NonNull String path) {
        val b = base(path).GET();

        addAuthHeaders(b);

        return send(b.build());
    }

    private CompletableFuture<XfResult> post(final @NonNull String path, final @NonNull String formBody) {
        val b = base(path)
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(formBody, StandardCharsets.UTF_8));

        addAuthHeaders(b);

        return send(b.build());
    }

    private HttpRequest.Builder base(final @NonNull String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(forumApi + path))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json");
    }

    private void addAuthHeaders(final @NonNull HttpRequest.Builder builder) {
        if (forumKey == null) {
            throw new IllegalStateException("XenForo API key is not set");
        }

        builder.header("XF-Api-Key", forumKey);
    }

    private CompletableFuture<XfResult> send(HttpRequest req) {
        return CLIENT.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(resp -> XfResult.from(resp.statusCode(), resp.body()))
                .exceptionally(XfResult::netError);
    }

    public record XfResult(boolean success, int status, String raw, XfResponse parsed) {
        static XfResult from(final int status, final String body) {
            boolean ok = status == 200;
            XfResponse parsed = null;

            try {
                parsed = GSON.fromJson(body, XfResponse.class);
            } catch (Exception ignored) {
            }

            return new XfResult(ok, status, body, parsed);
        }

        static XfResult netError(Throwable t) {
            return new XfResult(false, 0,
                    t == null ? "network_error" : t.getClass().getSimpleName() + ": " + t.getMessage(),
                    null);
        }

        public Integer userId() {
            return parsed != null && parsed.user != null ? parsed.user.userId : null;
        }
    }

    public static class XfResponse {
        boolean success;
        User user;
    }

    public static class User {
        @SerializedName("user_id")
        int userId;
        String username;
        String email;
    }
}
