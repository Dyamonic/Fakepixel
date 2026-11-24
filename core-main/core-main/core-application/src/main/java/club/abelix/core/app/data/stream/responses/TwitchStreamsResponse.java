package club.abelix.core.app.data.stream.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
public record TwitchStreamsResponse(
        @JsonProperty("data")       List<TwitchStreamsResponse.TwitchStreamData> data,
        @JsonProperty("pagination") TwitchStreamsResponse.Pagination              pagination,
        @JsonProperty("error")      String   error,
        @JsonProperty("status")     Integer  status,
        @JsonProperty("message")    String   message
) {
    public record TwitchStreamData(
            @JsonProperty("id")            String id,
            @JsonProperty("user_id")       String userId,
            @JsonProperty("user_login")    String userLogin,
            @JsonProperty("user_name")     String userName,
            @JsonProperty("game_id")       String gameId,
            @JsonProperty("game_name")     String gameName,
            @JsonProperty("type")          String type,
            @JsonProperty("title")         String title,
            @JsonProperty("viewer_count")  Integer viewerCount,
            @JsonProperty("started_at")    String startedAt,
            @JsonProperty("language")      String language,
            @JsonProperty("thumbnail_url") String thumbnailUrl,
            @JsonProperty("tag_ids")       List<String> tagIds,
            @JsonProperty("tags")          List<String> tags,
            @JsonProperty("is_mature")     Boolean isMature
    ) { }

    public record Pagination(
            @JsonProperty("cursor") String cursor
    ) { }
}
